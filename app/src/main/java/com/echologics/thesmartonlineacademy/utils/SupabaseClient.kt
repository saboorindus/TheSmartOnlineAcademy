package com.echologics.thesmartonlineacademy.utils

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import kotlin.time.Duration.Companion.seconds

object SupabaseClient {

    val supabase = createSupabaseClient(
        supabaseUrl = "https://vilzjwakvylaihhwitwi.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZpbHpqd2FrdnlsYWloaHdpdHdpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzYzNDc5MjMsImV4cCI6MjA5MTkyMzkyM30.UmsdUX-f7zAHo5z431eDXAOpWU7fS4A4nsuZYXagrx4"
    ) {
        install(Storage.Companion) {
            transferTimeout = 90.seconds // Default: 120 seconds
        }
    }
}