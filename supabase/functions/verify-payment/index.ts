import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const { order_id, payment_id, signature } = await req.json()

    if (!order_id || !payment_id || !signature) {
      return new Response(
        JSON.stringify({ error: "Missing required parameters: order_id, payment_id, signature" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    // 1. Fetch Razorpay Secret Key securely from environment variables
    const razorpaySecret = Deno.env.get("RAZORPAY_SECRET")
    if (!razorpaySecret) {
      return new Response(
        JSON.stringify({ error: "Razorpay Secret Key not configured on server environment variables." }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
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
    if (hashHex !== signature) {
      return new Response(
        JSON.stringify({ verified: false, error: "Signature mismatch! Payment verification failed." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    // 4. Initialize Supabase client inside Edge Function using Service Role Key (to bypass RLS for updates)
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? ""
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    const supabase = createClient(supabaseUrl, supabaseServiceKey)

    // 5. Reconcile transaction and mark the order as "accepted" (paid) in Supabase public.orders table
    const { error: dbError } = await supabase
      .from("orders")
      .update({ status: "accepted", updatedAt: Date.now() })
      .eq("id", order_id)

    if (dbError) {
      return new Response(
        JSON.stringify({ verified: true, error: `Signature verified, but database update failed: ${dbError.message}` }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    return new Response(
      JSON.stringify({ verified: true, message: `Payment verified and order accepted successfully! Transaction Ref: ${payment_id}` }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )

  } catch (error) {
    return new Response(
      JSON.stringify({ error: error.message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  }
})
