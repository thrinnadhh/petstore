import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.8"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  // Handle CORS preflight requests gracefully
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const supabaseUrl = Deno.env.get('SUPABASE_URL') ?? ""
    const supabaseServiceRoleKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ""
    
    // Construct the database client with elevated administrative rights to bypass RLS for keep-alive checks
    const supabase = createClient(supabaseUrl, supabaseServiceRoleKey)
    
    // Execute a lightweight ping query targeting the core structured profiles schema
    const { data, error } = await supabase
      .from('profiles')
      .select('id, role')
      .limit(1)
      
    if (error) {
      throw new Error(`Database query failed: ${error.message}`)
    }
    
    return new Response(
      JSON.stringify({ 
        success: true, 
        message: "⚡ [PawsNearMe Keep-Alive] Database pinged successfully! Prevents automatic free-tier suspension.",
        timestamp: new Date().toISOString(),
        recordsFound: data?.length ?? 0
      }),
      { 
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 200 
      }
    )
  } catch (error: any) {
    console.error(`🚨 [Keep-Alive Error]: ${error.message}`)
    return new Response(
      JSON.stringify({ 
        success: false, 
        error: error.message,
        timestamp: new Date().toISOString()
      }),
      { 
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 500 
      }
    )
  }
})
