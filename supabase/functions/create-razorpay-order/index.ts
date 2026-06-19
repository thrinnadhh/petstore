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

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders(req) })
  }

  try {
    if (req.method !== "POST") {
      return jsonResponse(req, { error: "Method not allowed" }, 405)
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? ""
    const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? ""
    const razorpayKeyId = Deno.env.get("RAZORPAY_KEY_ID") ?? ""
    const razorpaySecret = Deno.env.get("RAZORPAY_SECRET") ?? ""
    if (!supabaseUrl || !supabaseAnonKey || !razorpayKeyId || !razorpaySecret) {
      console.error("Payment order environment variables are not configured")
      return jsonResponse(req, { error: "Payment order creation is unavailable" }, 500)
    }

    const authHeader = req.headers.get("Authorization") ?? ""
    const supabase = createClient(supabaseUrl, supabaseAnonKey, {
      global: { headers: { Authorization: authHeader } },
    })
    const { data: userData, error: userError } = await supabase.auth.getUser()
    if (userError || !userData.user) {
      return jsonResponse(req, { error: "Unauthorized" }, 401)
    }

    const body = await req.json()
    const amount = Number(body.amount)
    const currency = typeof body.currency === "string" ? body.currency : "INR"
    const receipt = typeof body.receipt === "string" ? body.receipt : `receipt_${Date.now()}`

    if (!Number.isInteger(amount) || amount <= 0 || amount > 50000000 || currency !== "INR") {
      return jsonResponse(req, { error: "Invalid payment order request" }, 400)
    }

    const credentials = btoa(`${razorpayKeyId}:${razorpaySecret}`)
    const razorpayResponse = await fetch("https://api.razorpay.com/v1/orders", {
      method: "POST",
      headers: {
        "Authorization": `Basic ${credentials}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        amount,
        currency,
        receipt: receipt.slice(0, 40),
        payment_capture: 1,
      }),
    })

    const responseBody = await razorpayResponse.json()
    if (!razorpayResponse.ok || typeof responseBody.id !== "string") {
      console.error("Razorpay order creation failed", responseBody)
      return jsonResponse(req, { error: "Could not create payment order" }, 502)
    }

    return jsonResponse(req, {
      order_id: responseBody.id,
      amount: responseBody.amount,
      currency: responseBody.currency,
    })
  } catch (error) {
    console.error("Payment order creation error", error)
    return jsonResponse(req, { error: "Could not create payment order" }, 500)
  }
})
