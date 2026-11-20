package com.example.data.cloud

/**
 * Instant offline carrier, region, and verified Bangladesh helpline/service identification.
 * Works immediately even when offline or before cloud Caller ID responds.
 */
object CallerIdIntelligence {

    private val verifiedServices = mapOf(
        "999" to ("জাতীয় জরুরি সেবা (999)" to "Emergency • Bangladesh"),
        "333" to ("সরকারি তথ্য ও সেবা (333)" to "Govt Helpline • BD"),
        "109" to ("নারী ও শিশু নির্যাতন প্রতিরোধ" to "Helpline • Bangladesh"),
        "106" to ("দুদক হটলাইন (ACC)" to "Govt Helpline • BD"),
        "105" to ("জাতীয় পরিচয়পত্র (NID) সেবা" to "Govt Helpline • BD"),
        "102" to ("ফায়ার সার্ভিস ও সিভিল ডিফেন্স" to "Emergency • BD"),
        "16263" to ("স্বাস্থ্য বাতায়ন (Health)" to "Medical Helpline • BD"),
        "16247" to ("বিকাশ কাস্টমার কেয়ার (bKash)" to "Mobile Banking • BD"),
        "16167" to ("নগদ কাস্টমার কেয়ার (Nagad)" to "Mobile Banking • BD"),
        "16216" to ("রকেট / ডাচ-বাংলা ব্যাংক" to "Banking • BD"),
        "16268" to ("উপায় (Upay) কেয়ার" to "Mobile Banking • BD"),
        "16221" to ("ব্র্যাক ব্যাংক (BRAC Bank)" to "Banking • BD"),
        "16234" to ("সিটি ব্যাংক (City Bank)" to "Banking • BD"),
        "16259" to ("ইসলামী ব্যাংক (IBBL)" to "Banking • BD"),
        "16230" to ("ইস্টার্ন ব্যাংক (EBL)" to "Banking • BD"),
        "16123" to ("কৃষি কল সেন্টার" to "Agriculture • BD"),
        "16104" to ("বিটিসিএল / ব্রিলিয়ান্ট সাপোর্ট" to "Telecom • BD"),
        "121" to ("অপারেটর কাস্টমার কেয়ার" to "Telecom Support"),
        "158" to ("টেলিটক কাস্টমার কেয়ার" to "Teletalk • BD"),
        "09610001000" to ("ব্রিলিয়ান্ট কানেক্ট সাপোর্ট" to "Brilliant VoIP • BD"),
        "09612007007" to ("দারাজ বাংলাদেশ (Daraz)" to "E-Commerce • BD"),
        "09678100800" to ("পাঠাও (Pathao)" to "Ride & Delivery • BD"),
        "09612316247" to ("বিকাশ সাপোর্ট (bKash)" to "Mobile Banking • BD")
    )

    fun detectOffline(rawNumber: String): CallerIdLookupResult? {
        val key = toCanonicalPhoneKey(rawNumber)
        if (key.length < 3) return null

        val rawDigits = rawNumber.filter { it.isDigit() }
        verifiedServices[rawDigits]?.let { (serviceName, label) ->
            return CallerIdLookupResult(
                phoneKey = key,
                displayName = serviceName,
                carrierOrRegion = label,
                category = CallerCategory.BUSINESS,
                source = CallerIdSource.SERVICE_DIRECTORY
            )
        }

        val carrier = detectCarrierOrRegion(key)
        return CallerIdLookupResult(
            phoneKey = key,
            displayName = null,
            carrierOrRegion = carrier,
            category = CallerCategory.PERSONAL,
            source = CallerIdSource.CARRIER_ONLY
        )
    }

    fun detectCarrierOrRegion(phoneKey: String): String {
        val bdLocal = when {
            phoneKey.startsWith("880") && phoneKey.length >= 6 -> "0" + phoneKey.drop(3)
            phoneKey.startsWith("0") -> phoneKey
            else -> ""
        }

        if (bdLocal.isNotEmpty()) {
            return when {
                bdLocal.startsWith("017") || bdLocal.startsWith("013") -> "Grameenphone • Bangladesh"
                bdLocal.startsWith("018") -> "Robi • Bangladesh"
                bdLocal.startsWith("016") -> "Airtel • Bangladesh"
                bdLocal.startsWith("019") || bdLocal.startsWith("014") -> "Banglalink • Bangladesh"
                bdLocal.startsWith("015") -> "Teletalk • Bangladesh"
                bdLocal.startsWith("096") -> "IP Phone / Brilliant • BD"
                bdLocal.startsWith("02") -> "Dhaka Landline • BD"
                bdLocal.startsWith("031") -> "Chattogram Landline • BD"
                bdLocal.startsWith("0821") -> "Sylhet Landline • BD"
                bdLocal.startsWith("041") -> "Khulna Landline • BD"
                bdLocal.startsWith("0721") -> "Rajshahi Landline • BD"
                else -> "Bangladesh"
            }
        }

        return when {
            phoneKey.startsWith("91") -> "India (+91)"
            phoneKey.startsWith("966") -> "Saudi Arabia (+966)"
            phoneKey.startsWith("971") -> "UAE (+971)"
            phoneKey.startsWith("974") -> "Qatar (+974)"
            phoneKey.startsWith("965") -> "Kuwait (+965)"
            phoneKey.startsWith("968") -> "Oman (+968)"
            phoneKey.startsWith("60") -> "Malaysia (+60)"
            phoneKey.startsWith("65") -> "Singapore (+65)"
            phoneKey.startsWith("44") -> "United Kingdom (+44)"
            phoneKey.startsWith("1") && phoneKey.length == 11 -> "USA / Canada (+1)"
            else -> "Mobile Network"
        }
    }
}
