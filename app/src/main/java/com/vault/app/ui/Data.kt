package com.vault.app.ui

// ALL DATA BELOW IS FICTIONAL SAMPLE DATA for the prototype.
enum class Category(val label: String) { Identity("Identity"), Education("Education"), Financial("Financial"), Personal("Personal") }

data class Doc(val title: String, val category: Category, val masked: String?, val revealed: String?, val expiry: String?, val daysLeft: Int?, val accessed: String, val file: String? = null)
data class Contact(val name: String, val group: String, val phone: String, val emergency: Boolean = false)

val seedDocs = listOf(
    Doc("Aadhaar", Category.Identity, "XXXX XXXX 0000", "0000 0000 0000", null, null, "Today"),
    Doc("Passport", Category.Identity, "P•••••00", "P0000000", "Mar 2027", 45, "Yesterday"),
    Doc("PAN", Category.Identity, "•••••0000A", "AAAAA0000A", null, null, "3 days ago"),
    Doc("Class 12 Marksheet", Category.Education, null, null, null, null, "Last week"),
    Doc("Health Insurance Policy", Category.Financial, "••••0000", "POL00000000", "Aug 2027", 280, "Last week"),
    Doc("Rental Agreement", Category.Personal, null, null, "Jan 2027", 90, "2 weeks ago"),
)
val seedContacts = listOf(
    Contact("Sample Parent", "Family", "+910000000001", true),
    Contact("Dr. Sample", "Doctor", "+910000000002"),
    Contact("Bank Helpline", "Bank", "+910000000003"),
    Contact("Insurance Support", "Insurance", "+910000000004"),
)

// Live (decrypted) data; empty whenever VAULT is locked.
val sampleDocs: List<Doc> get() = Session.docs
val sampleContacts: List<Contact> get() = Session.contacts
