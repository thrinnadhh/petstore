package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// SWIGGY PAWS DESIGN SYSTEM COLOR TOKENS
// Vibrant Swiggy Orange × Deep Charcoal × Swiggy Fresh Green
// ============================================================================

// ── Core Swiggy Brand ───────────────────────────────────────────────────────
val SwiggyOrange       = Color(0xFFFC8019)   // Signature Vibrant Swiggy Orange
val SwiggyOrangeLight  = Color(0xFFFF9E59)   // Soft Glow Orange for gradients
val SwiggyCharcoal     = Color(0xFF282C3F)   // Main dark typography & dark background accent
val SwiggyDeepBlack    = Color(0xFF121212)   // Deepest dark background
val SwiggyDarkSurface  = Color(0xFF1E1E1E)   // Dark surface / card background
val SwiggyGreen        = Color(0xFF60B246)   // Swiggy Green for ratings & success states
val SwiggyAmber        = Color(0xFFDB7C00)   // Warm Amber secondary highlights
val SwiggyLightGrey    = Color(0xFFF4F4F5)   // Light Grey background
val SwiggySoftWhite    = Color(0xFFFAFAFA)   // White off-canvas

// ── Status Utilities ─────────────────────────────────────────────────────────
val StatusSuccess      = Color(0xFF60B246)   // Swiggy Green for Open / Success
val StatusError        = Color(0xFFF44336)   // Red for Closed / Error
val StatusWarning      = Color(0xFFFFB300)   // Amber for Warnings
val StatusInfo         = Color(0xFF5BBCD9)   // Sky Blue for chats

// ============================================================================
// LIGHT MODE COLOR SCHEME
// ============================================================================
val LightPrimary        = SwiggyOrange
val LightSecondary      = SwiggyCharcoal
val LightTertiary       = SwiggyGreen
val LightBackground     = Color(0xFFF9F9FB)   // Soft off-white Swiggy background
val LightSurface        = Color(0xFFFFFFFF)   // Pure white crisp cards
val LightOnPrimary      = Color.White
val LightOnSecondary    = Color.White
val LightOnBackground   = SwiggyCharcoal
val LightOnSurface      = SwiggyCharcoal

// ============================================================================
// DARK MODE COLOR SCHEME
// ============================================================================
val DarkPrimary         = SwiggyOrange        // Swiggy Orange glows on dark backgrounds
val DarkSecondary       = SwiggyOrangeLight
val DarkTertiary        = SwiggyGreen
val DarkBackground      = SwiggyDeepBlack     // Black mode
val DarkSurface         = SwiggyDarkSurface   // Dark charcoal cards
val DarkOnPrimary       = Color.White
val DarkOnSecondary     = SwiggyDeepBlack
val DarkOnBackground    = SwiggySoftWhite
val DarkOnSurface       = SwiggySoftWhite
