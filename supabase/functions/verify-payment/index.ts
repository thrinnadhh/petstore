import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

function corsHeaders(req: Request) {
  const allowedOrigin = Deno.env.get("ALLOWED_ORIGIN") ?? "*"
  const requestOrigin = req.headers.get("origin") ?? ""
  return {
    "Access-Control-Allow-Origin": allowedOrigin === "*" ? "*" : (requestOrigin === allowedOrigin ? requestOrigin : allowedOrigin),
    "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  }
}

function jsonResponse(req: Request, body: Record<string, unknown>, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders(req), "Content-Type": "application/json" },
  })
}

function constantTimeEquals(a: string, b: string) {
  if (a.length !== b.length) return false
  let diff = 0
  for (let i = 0; i < a.length; i++) {
    diff |= a.charCodeAt(i) ^ b.charCodeAt(i)
  }
  return diff === 0
}

serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders(req) })
  }

  try {
    if (req.method !== "POST") {
      return jsonResponse(req, { error: "Method not allowed" }, 405)
    }

    const { order_id, payment_id, signature } = await req.json()

    if (
      typeof order_id !== "string" ||
      typeof payment_id !== "string" ||
      typeof signature !== "string" ||
      !order_id.trim() ||
      !payment_id.trim() ||
      !signature.trim()
    ) {
      return jsonResponse(req, { error: "Invalid payment verification request" }, 400)
    }

    // 1. Fetch Razorpay Secret Key securely from environment variables
    const razorpaySecret = Deno.env.get("RAZORPAY_SECRET")
    if (!razorpaySecret) {
      console.error("RAZORPAY_SECRET is not configured")
      return jsonResponse(req, { error: "Payment verification is unavailable" }, 500)
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? ""
    const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? ""
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    if (!supabaseUrl || !supabaseAnonKey || !supabaseServiceKey) {
      console.error("Supabase environment variables are not configured")
      return jsonResponse(req, { error: "Payment verification is unavailable" }, 500)
    }

    const authHeader = req.headers.get("Authorization") ?? ""
    const userClient = createClient(supabaseUrl, supabaseAnonKey, {
      global: { headers: { Authorization: authHeader } },
    })
    const { data: userData, error: userError } = await userClient.auth.getUser()
    if (userError || !userData.user) {
      return jsonResponse(req, { error: "Unauthorized" }, 401)
    }

    // 2. Compute HMAC-SHA256 signature locally: order_id + "|" + payment_id
    const secretBytes = new TextEncoder().encode(razorpaySecret)
    const textBytes = new TextEncoder().encode(`${order_id}|${payment_id}`)

    const key = await crypto.subtle.importKey(
      "raw",
      secretBytes,
      { name: "HMAC", hash: "SHA-256" },
      false,
      ["sign"]
    )
    const signatureBuffer = await crypto.subtle.sign("HMAC", key, textBytes)
    
    // Convert signature bytes to hex string
    const hashHex = Array.from(new Uint8Array(signatureBuffer))
      .map((b) => b.toString(16).padStart(2, "0"))
      .join("")

    // 3. Verify signature matches the client-provided signature
    if (!constantTimeEquals(hashHex, signature)) {
      return jsonResponse(req, { verified: false, error: "Payment verification failed" }, 400)
    }

    // 4. Initialize Supabase client inside Edge Function using Service Role Key (to bypass RLS for updates)
    const supabase = createClient(supabaseUrl, supabaseServiceKey)

    // 5. Reconcile transaction and mark the order as "accepted" (paid) in Supabase public.orders table
    const { error: dbError } = await supabase
      .from("orders")
      .update({ status: "accepted", updatedAt: Date.now() })
      .eq("id", order_id)

    if (dbError) {
      console.error("Payment verified but order update failed", dbError.message)
      return jsonResponse(req, { verified: true, error: "Payment verified, but order update failed" }, 500)
    }

    return jsonResponse(req, { verified: true, message: "Payment verified and order accepted successfully" })

  } catch (error) {
    console.error("Payment verification error", error)
    return jsonResponse(req, { error: "Payment verification failed" }, 500)
  }
})
