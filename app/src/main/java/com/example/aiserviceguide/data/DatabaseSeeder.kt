package com.example.aiserviceguide.data

import androidx.room3.withWriteTransaction

object DatabaseSeeder {

    suspend fun seedDatabase(database: AppDatabase) {

        val services = listOf(

            Service(
                serviceId = "S001",
                serviceName = "Birth Certificate",
                description = "Birth and birth-registration related government service.",
                jurisdiction = "Karnataka",
                department = "Birth/Death Registration",
                officialSource = "https://services.india.gov.in/service/detail/apply-for-births-deaths-and-marriage-related-services-karnataka",
                sourceConfidence = "High"
            ),

            Service(
                serviceId = "S002",
                serviceName = "Death Certificate",
                description = "Death certificate and death-registration related government service.",
                jurisdiction = "Karnataka",
                department = "Birth/Death Registration",
                officialSource = "https://services.india.gov.in/service/detail/apply-for-births-deaths-and-marriage-related-services-karnataka",
                sourceConfidence = "High"
            ),

            Service(
                serviceId = "S003",
                serviceName = "Income Certificate",
                description = "Government service for obtaining an income certificate.",
                jurisdiction = "Karnataka",
                department = "Revenue / Citizen Services",
                officialSource = "https://services.india.gov.in/service/detail/online-application-for-income-certificate",
                sourceConfidence = "Medium"
            ),

            Service(
                serviceId = "S004",
                serviceName = "Residence / Domicile Certificate",
                description = "Government service related to residence or domicile certification.",
                jurisdiction = "Karnataka",
                department = "Revenue",
                officialSource = "https://bidar.nic.in/en/service/residence-certificate/",
                sourceConfidence = "High"
            ),

            Service(
                serviceId = "S005",
                serviceName = "Caste Certificate",
                description = "Government service related to caste/community certification.",
                jurisdiction = "Karnataka",
                department = "Revenue / Social Welfare",
                officialSource = "https://bidar.nic.in/en/service/caste-certificate/",
                sourceConfidence = "High"
            ),

            Service(
                serviceId = "S006",
                serviceName = "Ration Card Services",
                description = "Government services related to ration cards and family beneficiary information.",
                jurisdiction = "Karnataka",
                department = "Food & Civil Supplies",
                officialSource = "https://ahara.kar.nic.in/Home/EServices",
                sourceConfidence = "High"
            ),

            Service(
                serviceId = "S007",
                serviceName = "Driving Licence Services",
                description = "Government transport services related to driving licences.",
                jurisdiction = "Karnataka",
                department = "Transport Department / RTO",
                officialSource = "https://sarathi.parivahan.gov.in/",
                sourceConfidence = "High"
            ),

            Service(
                serviceId = "S008",
                serviceName = "Vehicle Registration / RC Services",
                description = "Vehicle registration and registration-certificate related services.",
                jurisdiction = "Karnataka",
                department = "Transport Department / RTO",
                officialSource = "https://parivahan.gov.in/en/content/permanent-registration",
                sourceConfidence = "High"
            ),

            Service(
                serviceId = "S009",
                serviceName = "Passport Application",
                description = "Fresh or reissue passport application service.",
                jurisdiction = "India",
                department = "Passport Seva / MEA",
                officialSource = "https://www.passportindia.gov.in/psp/Apply",
                sourceConfidence = "High"
            ),

            Service(
                serviceId = "S010",
                serviceName = "Aadhaar Enrolment / Update",
                description = "Aadhaar enrolment and update related service.",
                jurisdiction = "India",
                department = "UIDAI",
                officialSource = "https://uidai.gov.in/en/my-aadhaar",
                sourceConfidence = "High"
            )
        )


        val documents = listOf(

            Document(
                documentId = "D001",
                documentName = "Aadhaar / e-Aadhaar",
                purpose = "Identity / address depending on service",
                sourceAuthority = "UIDAI",
                verificationNote = "Verify per service"
                ,
                howToObtain = "Use the official UIDAI enrolment/update workflow and follow the accepted supporting-document rules for the selected Aadhaar service.",
                whereToObtain = "UIDAI My Aadhaar services or an authorised Aadhaar Enrolment/Update Centre.",
                officialUrl = "https://uidai.gov.in/en/my-aadhaar"
            ),

            Document(
                documentId = "D002",
                documentName = "Proof of Address",
                purpose = "Address evidence",
                sourceAuthority = "Service-specific official source",
                verificationNote = "Do not assume a universal list"
                ,
                howToObtain = "For the Karnataka Income Certificate workflow used by this project, the official Bidar district page lists Ration Card or Election Identity Card among the documents to be enclosed. It also lists attested PAN Card or Driving Licence. Use an accepted document from the official service list.",
                whereToObtain = "Ration Card: use the Karnataka Food & Civil Supplies service. Election Identity Card: use the Election Commission voter services. Driving Licence: use the official Parivahan service. The exact document accepted for the citizen's application must be checked against the service source.",
                officialUrl = ""
            ),

            Document(
                documentId = "D003",
                documentName = "Proof of Identity",
                purpose = "Identity evidence",
                sourceAuthority = "Service-specific official source",
                verificationNote = "Do not assume a universal list"
                ,
                howToObtain = "Use an identity document accepted by the selected service. The exact accepted list must be taken from that service's official source.",
                whereToObtain = "Obtain the accepted identity document from its issuing authority.",
                officialUrl = ""
            ),

            Document(
                documentId = "D004",
                documentName = "Photograph",
                purpose = "Photo requirement where applicable",
                sourceAuthority = "Service-specific official source",
                verificationNote = "Verify size/format"
                ,
                howToObtain = "Obtain a recent photograph that matches the exact size, format and background rules of the selected service.",
                whereToObtain = "A photo studio or other provider can prepare the photograph; the service's official rules must be followed.",
                officialUrl = ""
            ),

            Document(
                documentId = "D005",
                documentName = "Birth Certificate / proof of date of birth",
                purpose = "Date of birth evidence",
                sourceAuthority = "Passport Seva / relevant state source",
                verificationNote = "Verify category-specific rules"
                ,
                howToObtain = "Obtain the required birth/date-of-birth document from the competent issuing authority and verify the category-specific rule for the service.",
                whereToObtain = "Birth & Death Registrar or other competent authority specified by the service.",
                officialUrl = "https://www.passportindia.gov.in/psp/Apply"
            ),

            Document(
                documentId = "D006",
                documentName = "Income Evidence",
                purpose = "Income evidence",
                sourceAuthority = "State service source",
                verificationNote = "Exact accepted documents vary"
                ,
                howToObtain = "Obtain the income evidence accepted by the selected service. The exact evidence must be verified for that service.",
                whereToObtain = "Use the authority or issuing source named in the selected service's verified requirements.",
                officialUrl = ""
            ),

            Document(
                documentId = "D007",
                documentName = "Caste/Community Evidence",
                purpose = "Caste/community evidence",
                sourceAuthority = "State service source",
                verificationNote = "Exact accepted documents vary"
                ,
                howToObtain = "Obtain the caste/community evidence accepted by the selected service.",
                whereToObtain = "Use the competent state authority specified by the selected service.",
                officialUrl = ""
            ),

            Document(
                documentId = "D008",
                documentName = "Existing Driving Licence",
                purpose = "Existing licence",
                sourceAuthority = "Parivahan / state transport source",
                verificationNote = "Relevant to renewal/duplicate services"
                ,
                howToObtain = "Use the existing driving licence for services where it is verified as applicable.",
                whereToObtain = "Parivahan/state transport authority or the holder's existing licence records.",
                officialUrl = "https://parivahan.gov.in/"
            ),

            Document(
                documentId = "D009",
                documentName = "Vehicle Registration / ownership documents",
                purpose = "Vehicle evidence",
                sourceAuthority = "Parivahan / state transport source",
                verificationNote = "Service-specific"
                ,
                howToObtain = "Collect the vehicle registration/ownership documents required for the selected transport workflow.",
                whereToObtain = "Parivahan or the relevant state transport authority.",
                officialUrl = "https://parivahan.gov.in/"
            ),

            Document(
                documentId = "D010",
                documentName = "Ration Card / family details",
                purpose = "Family/beneficiary evidence",
                sourceAuthority = "State food & civil supplies source",
                verificationNote = "Service-specific"
                ,
                howToObtain = "Use the ration card/family details required for the selected food and civil supplies workflow.",
                whereToObtain = "Karnataka Food & Civil Supplies / the relevant official state service.",
                officialUrl = ""
            ),
            Document(
                documentId = "D016",
                documentName = "Residence Proof (Ration Card / Voter ID / School Documents)",
                purpose = "Evidence of residence",
                sourceAuthority = "Bidar District Administration, Government of Karnataka",
                verificationNote = "The official Residence Certificate page lists ration card, election voter ID card or school documents.",
                howToObtain = "Use one of the residence documents accepted by the official Residence Certificate workflow: ration card, election voter ID card or applicable school documents.",
                whereToObtain = "Use the issuing authority for the selected document. The Residence Certificate itself is handled through the Karnataka citizen-service/revenue workflow.",
                officialUrl = "https://bidar.nic.in/en/service/residence-certificate/"
            ),

            Document(
                documentId = "D017",
                documentName = "Rent Agreement",
                purpose = "Residential occupancy evidence",
                sourceAuthority = "Bidar District Administration, Government of Karnataka",
                verificationNote = "The official Residence Certificate page lists a copy of the rent agreement among documents to be enclosed.",
                howToObtain = "Provide a copy of the applicable rent agreement for the residence.",
                whereToObtain = "From the existing rental agreement/landlord documentation, as applicable.",
                officialUrl = "https://bidar.nic.in/en/service/residence-certificate/"
            ),

            Document(
                documentId = "D018",
                documentName = "Caste Evidence (School / Transfer / Cumulative Record)",
                purpose = "Caste/community evidence",
                sourceAuthority = "Bidar District Administration, Government of Karnataka",
                verificationNote = "The official Caste Certificate page lists caste information in primary school certificate, transfer certificate or cumulative record.",
                howToObtain = "Use the relevant school or educational record containing the caste information specified by the official workflow.",
                whereToObtain = "The school or educational institution that issued the relevant record.",
                officialUrl = "https://bidar.nic.in/en/service/caste-certificate/"
            ),

            Document(
                documentId = "D019",
                documentName = "Parent Caste Certificate / Service Register Evidence",
                purpose = "Parent/family caste evidence",
                sourceAuthority = "Bidar District Administration, Government of Karnataka",
                verificationNote = "The official workflow includes parent caste/service-register evidence in applicable cases.",
                howToObtain = "Provide the parent caste certificate or applicable caste entry in the service register when the official workflow condition applies.",
                whereToObtain = "The issuing authority holding the parent's service or caste record.",
                officialUrl = "https://bidar.nic.in/en/service/caste-certificate/"
            ),

            Document(
                documentId = "D020",
                documentName = "Caste Affidavit",
                purpose = "Applicant declaration for caste certificate",
                sourceAuthority = "Bidar District Administration, Government of Karnataka",
                verificationNote = "The official Caste Certificate page lists an affidavit on Rs.20 bond paper.",
                howToObtain = "Prepare the affidavit in the form and value specified by the official service workflow and complete the required attestation/notarial formalities where applicable.",
                whereToObtain = "Use the competent notarial/attestation service as applicable to the official application process.",
                officialUrl = "https://bidar.nic.in/en/service/caste-certificate/"
            ),

            Document(
                documentId = "D021",
                documentName = "Election Voter ID Card",
                purpose = "Identity evidence for caste certificate workflow",
                sourceAuthority = "Bidar District Administration, Government of Karnataka",
                verificationNote = "The official Caste Certificate page lists election voter ID card among the documents to be enclosed.",
                howToObtain = "Use a valid voter ID/electoral identity document issued through the election authorities.",
                whereToObtain = "Election Commission/Chief Electoral Officer services.",
                officialUrl = "https://bidar.nic.in/en/service/caste-certificate/"
            ),

            Document(
                documentId = "D022",
                documentName = "Family Tree",
                purpose = "Family relationship evidence",
                sourceAuthority = "Bidar District Administration, Government of Karnataka",
                verificationNote = "The official Caste Certificate page lists family tree among the documents to be enclosed.",
                howToObtain = "Obtain or prepare the family-tree record through the competent local/revenue authority as applicable.",
                whereToObtain = "The competent local/revenue authority handling family records.",
                officialUrl = "https://bidar.nic.in/en/service/caste-certificate/"
            ),

            Document(
                documentId = "D023",
                documentName = "Residential Certificate",
                purpose = "Residence evidence for caste certificate",
                sourceAuthority = "Bidar District Administration, Government of Karnataka",
                verificationNote = "The official Caste Certificate page lists a residential certificate issued by revenue officers.",
                howToObtain = "Obtain the residential certificate through the competent revenue authority.",
                whereToObtain = "Revenue authority/Tahsildar office or the applicable Karnataka citizen-service route.",
                officialUrl = "https://bidar.nic.in/en/service/caste-certificate/"
            ),

            Document(
                documentId = "D024",
                documentName = "Aadhaar-linked Mobile Number",
                purpose = "Online ration-card service authentication/contact",
                sourceAuthority = "Karnataka Food & Civil Supplies / Ahara",
                verificationNote = "A Karnataka government-hosted scheme document lists Aadhaar-linked mobile number among BPL ration-card application documents.",
                howToObtain = "Use the mobile number linked to the applicant's Aadhaar record for the applicable ration-card workflow.",
                whereToObtain = "Use UIDAI/Aadhaar update services if the mobile number needs to be updated.",
                officialUrl = "https://ahara.kar.nic.in/Home/EServices"
            ),

            Document(
                documentId = "D025",
                documentName = "Ration Card Address Proof",
                purpose = "Residence/address evidence for ration-card application",
                sourceAuthority = "Karnataka Food & Civil Supplies",
                verificationNote = "Karnataka government sources identify address/domicile proof as part of ration-card enrolment workflows.",
                howToObtain = "Use a valid address document accepted by the Karnataka ration-card workflow.",
                whereToObtain = "Use the issuing authority for the selected address document.",
                officialUrl = "https://ahara.kar.nic.in/Home/EServices"
            ),

            Document(
                documentId = "D026",
                documentName = "Family Member Details",
                purpose = "Household/family information for ration-card enrolment",
                sourceAuthority = "Karnataka Food & Civil Supplies",
                verificationNote = "Karnataka district government guidance states that family members are to be furnished/participate in ration-card enrolment.",
                howToObtain = "Prepare the required details of all household members for the selected ration-card application.",
                whereToObtain = "Use the family's official identity and household records.",
                officialUrl = "https://ahara.kar.nic.in/Home/EServices"
            ),

            Document(
                documentId = "D027",
                documentName = "Learner's Licence",
                purpose = "Prerequisite for new permanent driving licence",
                sourceAuthority = "Parivahan / Sarathi",
                verificationNote = "Parivahan states that non-diplomat applicants normally need a Learner's Licence before applying for a new Driving Licence.",
                howToObtain = "Apply for a Learner's Licence through the Sarathi Parivahan portal, select Karnataka and complete the applicable workflow.",
                whereToObtain = "Sarathi Parivahan portal and the concerned RTO for scheduled in-person requirements.",
                officialUrl = "https://sarathi.parivahan.gov.in/"
            ),

            Document(
                documentId = "D028",
                documentName = "Medical / Fitness Form (Form 1 or Form 1A as applicable)",
                purpose = "Medical fitness declaration for driving-licence workflow",
                sourceAuthority = "Parivahan / Ministry of Road Transport and Highways",
                verificationNote = "Parivahan FAQ states Form 1 is used for non-transport applicants and Form 1A is required for applicants aged 40 or above; transport-vehicle cases have additional rules.",
                howToObtain = "Complete the medical/fitness form applicable to the applicant and vehicle category under the Parivahan workflow.",
                whereToObtain = "Use the prescribed form and, where required, the authorised medical practitioner/medical facility specified by the transport workflow.",
                officialUrl = "https://parivahan.gov.in/"
            ),

            Document(
                documentId = "D029",
                documentName = "Vehicle Sales Certificate (Form 21)",
                purpose = "Proof of vehicle sale for permanent registration",
                sourceAuthority = "Parivahan",
                verificationNote = "Parivahan permanent-registration guidance lists the sales certificate in Form 21.",
                howToObtain = "Obtain Form 21 from the vehicle dealer/manufacturer as part of the vehicle purchase documentation.",
                whereToObtain = "The authorised dealer/manufacturer supplying the vehicle.",
                officialUrl = "https://parivahan.gov.in/en/content/permanent-registration"
            ),

            Document(
                documentId = "D030",
                documentName = "Vehicle Roadworthiness Certificate (Form 22)",
                purpose = "Vehicle roadworthiness evidence",
                sourceAuthority = "Parivahan",
                verificationNote = "Parivahan permanent-registration guidance lists Form 22 from the manufacturer (and Form 22A from the body builder where applicable).",
                howToObtain = "Obtain the applicable roadworthiness certificate from the manufacturer/body builder.",
                whereToObtain = "Vehicle manufacturer/body builder.",
                officialUrl = "https://parivahan.gov.in/en/content/permanent-registration"
            ),

            Document(
                documentId = "D031",
                documentName = "Vehicle Pollution Under Control Certificate",
                purpose = "Vehicle emissions compliance",
                sourceAuthority = "Parivahan",
                verificationNote = "Parivahan permanent-registration guidance lists a Pollution Under Control certificate.",
                howToObtain = "Obtain a valid PUC certificate for the vehicle where required.",
                whereToObtain = "An authorised PUC testing centre.",
                officialUrl = "https://parivahan.gov.in/en/content/permanent-registration"
            ),

            Document(
                documentId = "D032",
                documentName = "Vehicle Insurance Certificate",
                purpose = "Valid motor insurance evidence",
                sourceAuthority = "Parivahan",
                verificationNote = "Parivahan permanent-registration guidance lists a valid insurance certificate.",
                howToObtain = "Obtain valid motor vehicle insurance from an authorised insurer.",
                whereToObtain = "The vehicle insurer/authorised insurance provider.",
                officialUrl = "https://parivahan.gov.in/en/content/permanent-registration"
            ),

            Document(
                documentId = "D033",
                documentName = "Passport Residence Proof",
                purpose = "Present-address evidence for passport application",
                sourceAuthority = "Passport Seva / Ministry of External Affairs",
                verificationNote = "Passport Seva lists proof of residence as a basic supporting-document category.",
                howToObtain = "Use an accepted present-address document from the Passport Seva document list.",
                whereToObtain = "Use the issuing authority for the accepted residence document.",
                officialUrl = "https://www.passportindia.gov.in/psp/GettingStarted"
            ),

            Document(
                documentId = "D034",
                documentName = "Passport Identity Proof with Photograph",
                purpose = "Identity verification for passport application",
                sourceAuthority = "Passport Seva / Ministry of External Affairs",
                verificationNote = "Passport Seva lists identity proof with photograph as a basic supporting-document category.",
                howToObtain = "Use an accepted identity document with photograph from the Passport Seva document guidance.",
                whereToObtain = "Use the issuing authority for the accepted identity document.",
                officialUrl = "https://www.passportindia.gov.in/psp/GettingStarted"
            ),

            Document(
                documentId = "D035",
                documentName = "Passport Nationality Evidence",
                purpose = "Nationality verification for passport application",
                sourceAuthority = "Passport Seva / Ministry of External Affairs",
                verificationNote = "Passport Seva states that proof of nationality is verified from the supporting documents submitted.",
                howToObtain = "Provide the nationality evidence accepted for the applicant category by Passport Seva.",
                whereToObtain = "Use the competent issuing authority and the Passport Seva document-advisory rules.",
                officialUrl = "https://www.passportindia.gov.in/psp/GettingStarted"
            ),

            Document(
                documentId = "D037",
                documentName = "Vehicle Registration Proof of Address",
                purpose = "Address evidence for permanent vehicle registration",
                sourceAuthority = "Parivahan",
                verificationNote = "Parivahan permanent-registration guidance lists proof of address.",
                howToObtain = "Use a valid proof of address accepted for the registration workflow.",
                whereToObtain = "Use the issuing authority for the selected address document.",
                officialUrl = "https://parivahan.gov.in/en/content/permanent-registration"
            ),

            Document(
                documentId = "D036",
                documentName = "Aadhaar Supporting Documents (PoI / PoA / PoR / DoB as applicable)",
                purpose = "Supporting evidence for Aadhaar enrolment/update",
                sourceAuthority = "UIDAI",
                verificationNote = "UIDAI publishes an official list of acceptable documents for Proof of Identity, Address, Relationship and Date of Birth; the exact combination depends on the enrolment/update case.",
                howToObtain = "Select an acceptable document from the current UIDAI supporting-document list for the specific Aadhaar enrolment/update request.",
                whereToObtain = "Use the issuing authority for the selected document and the nearest authorised Aadhaar Enrolment/Update Centre as applicable.",
                officialUrl = "https://www.uidai.gov.in/en/my-aadhaar"
            ),

            Document(
                documentId = "D011",
                documentName = "Death Details",
                purpose = "Death-event information",
                sourceAuthority = "Karnataka Death Certificate service",
                verificationNote = "Required death details include mother name, father name, place of death and date of death.",
                howToObtain = "Provide the required details of the deceased and the death event as specified by the official death-certificate workflow.",
                whereToObtain = "Use the information available from the family, medical institution or registration records, as applicable.",
                officialUrl = "https://bidar.nic.in/en/service/apply-for-death-certificates/"
            ),

            Document(
                documentId = "D012",
                documentName = "FIR",
                purpose = "Evidence for unnatural death",
                sourceAuthority = "Police Department / official death-certificate workflow",
                verificationNote = "Applicable only in case of unnatural death.",
                howToObtain = "Obtain the FIR from the police authority handling the unnatural-death case.",
                whereToObtain = "The concerned police station or investigating authority.",
                officialUrl = "https://bidar.nic.in/en/service/apply-for-death-certificates/"
            ),

            Document(
                documentId = "D013",
                documentName = "Postmortem Report",
                purpose = "Evidence for unnatural death",
                sourceAuthority = "Medical / forensic authority",
                verificationNote = "Applicable only in case of unnatural death.",
                howToObtain = "Obtain the postmortem report from the authority responsible for the postmortem.",
                whereToObtain = "The concerned hospital or medical/forensic authority.",
                officialUrl = "https://bidar.nic.in/en/service/apply-for-death-certificates/"
            ),

            Document(
                documentId = "D014",
                documentName = "Death Certificate / Details from Medical Officer",
                purpose = "Institutional death evidence",
                sourceAuthority = "Medical Officer / institution",
                verificationNote = "Applicable in case of institutional death.",
                howToObtain = "Obtain the death certificate or required death details from the Medical Officer as specified by the official workflow.",
                whereToObtain = "The concerned medical institution.",
                officialUrl = "https://bidar.nic.in/en/service/apply-for-death-certificates/"
            ),

            Document(
                documentId = "D015",
                documentName = "Headman / Home Death Information",
                purpose = "Evidence for death at home",
                sourceAuthority = "Official death-certificate workflow",
                verificationNote = "Applicable in case of death at home.",
                howToObtain = "Provide the information/documentation from the headman of the home as specified by the official service workflow.",
                whereToObtain = "From the headman of the home, as applicable.",
                officialUrl = "https://bidar.nic.in/en/service/apply-for-death-certificates/"
            ),

            Document(
                documentId = "D038",
                documentName = "Birth Details",
                purpose = "Birth-event information",
                sourceAuthority = "Bidar District, Government of Karnataka",
                verificationNote = "The official Birth Certificate service lists mother name, father name, place of birth and date of birth as birth details to be enclosed.",
                howToObtain = "Provide the mother name, father name, place of birth and date of birth from the family and available birth records.",
                whereToObtain = "Use the family/birth records and the local birth-registration authority as applicable.",
                officialUrl = "https://bidar.nic.in/en/service/birth-certificate/"
            ),
        )


        val serviceDocuments = listOf(

            ServiceDocument(
                serviceId = "S001",
                documentId = "D003",
                status = "REQUIRED",
                notes = "The official Karnataka Birth Certificate service lists ID Proof as a document to be enclosed.",
                verifiedAlternative = "",
                sourceUrl = "https://bidar.nic.in/en/service/birth-certificate/"
            ),

            ServiceDocument(
                serviceId = "S001",
                documentId = "D002",
                status = "REQUIRED",
                notes = "The official Karnataka Birth Certificate service lists Address Proof as a document to be enclosed.",
                verifiedAlternative = "",
                sourceUrl = "https://bidar.nic.in/en/service/birth-certificate/"
            ),

            ServiceDocument(
                serviceId = "S001",
                documentId = "D038",
                status = "REQUIRED",
                notes = "The official Karnataka Birth Certificate service lists birth details: mother name, father name, place of birth and date of birth.",
                verifiedAlternative = "",
                sourceUrl = "https://bidar.nic.in/en/service/birth-certificate/"
            ),

            ServiceDocument(
                serviceId = "S002",
                documentId = "D003",
                status = "REQUIRED",
                notes = "Proof of identity is required for the Death Certificate workflow.",
                verifiedAlternative = "",
                sourceUrl = "https://bidar.nic.in/en/service/apply-for-death-certificates/"
            ),

            ServiceDocument(
                serviceId = "S002",
                documentId = "D002",
                status = "REQUIRED",
                notes = "Proof of address is required for the Death Certificate workflow.",
                verifiedAlternative = "",
                sourceUrl = "https://bidar.nic.in/en/service/apply-for-death-certificates/"
            ),

            ServiceDocument(
                serviceId = "S002",
                documentId = "D011",
                status = "REQUIRED",
                notes = "Death details are required: mother name, father name, place of death and date of death.",
                verifiedAlternative = "",
                sourceUrl = "https://bidar.nic.in/en/service/apply-for-death-certificates/"
            ),

            ServiceDocument(
                serviceId = "S002",
                documentId = "D012",
                status = "REQUIRED",
                notes = "Only applicable for an unnatural death. FIR is required according to the official workflow.",
                verifiedAlternative = "",
                sourceUrl = "https://bidar.nic.in/en/service/apply-for-death-certificates/"
            ),

            ServiceDocument(
                serviceId = "S002",
                documentId = "D013",
                status = "REQUIRED",
                notes = "Only applicable for an unnatural death. Postmortem report is required according to the official workflow.",
                verifiedAlternative = "",
                sourceUrl = "https://bidar.nic.in/en/service/apply-for-death-certificates/"
            ),

            ServiceDocument(
                serviceId = "S002",
                documentId = "D014",
                status = "REQUIRED",
                notes = "Only applicable for an institutional death. Medical Officer death certificate/details are required.",
                verifiedAlternative = "",
                sourceUrl = "https://bidar.nic.in/en/service/apply-for-death-certificates/"
            ),

            ServiceDocument(
                serviceId = "S002",
                documentId = "D015",
                status = "REQUIRED",
                notes = "Only applicable when the death occurred at home. Headman/home information is required.",
                verifiedAlternative = "",
                sourceUrl = "https://bidar.nic.in/en/service/apply-for-death-certificates/"
            ),

            ServiceDocument(
                serviceId = "S003",
                documentId = "D003",
                status = "REQUIRED",
                notes = "Identity evidence is listed in the official Karnataka district Income Certificate workflow (for example Election Identity Card)."
                ,
                verifiedAlternative = "",
                sourceUrl = "https://bidar.nic.in/service/income-certificate/"
            ),

            ServiceDocument(
                serviceId = "S003",
                documentId = "D002",
                status = "REQUIRED",
                notes = "The official Karnataka district Income Certificate workflow lists Ration Card or Election Identity Card among documents to be enclosed; this relationship is used as the project's verified address/residence-document requirement."
                ,
                verifiedAlternative = "Ration Card or Election Identity Card; the same official page also lists attested PAN Card or Driving Licence.",
                sourceUrl = "https://bidar.nic.in/service/income-certificate/"
            ),

            ServiceDocument(
                serviceId = "S003",
                documentId = "D006",
                status = "REQUIRED",
                notes = "The official Karnataka district Income Certificate workflow requires salary evidence for government/private employees and asks the applicant to state family income and sources."
                ,
                verifiedAlternative = "",
                sourceUrl = "https://bidar.nic.in/service/income-certificate/"
            ),

            ServiceDocument(
                serviceId = "S004",
                documentId = "D016",
                status = "REQUIRED",
                notes = "The official Residence Certificate page lists ration card, election voter ID card or school documents as residence evidence.",
                verifiedAlternative = "Ration Card OR Election Voter ID Card OR School Documents.",
                sourceUrl = "https://bidar.nic.in/en/service/residence-certificate/"
            ),

            ServiceDocument(
                serviceId = "S004",
                documentId = "D017",
                status = "REQUIRED",
                notes = "The official Residence Certificate page lists a copy of the rent agreement.",
                verifiedAlternative = "",
                sourceUrl = "https://bidar.nic.in/en/service/residence-certificate/"
            ),

            ServiceDocument(
                serviceId = "S005",
                documentId = "D018",
                status = "REQUIRED",
                notes = "Caste information in primary school certificate, transfer certificate or cumulative record is listed by the official service page.",
                verifiedAlternative = "Primary School Certificate OR Transfer Certificate OR Cumulative Record, as applicable.",
                sourceUrl = "https://bidar.nic.in/en/service/caste-certificate/"
            ),

            ServiceDocument(
                serviceId = "S005",
                documentId = "D019",
                status = "REQUIRED",
                notes = "Parent caste/service-register evidence is listed for applicable cases.",
                verifiedAlternative = "Parent caste certificate OR applicable caste entry in service register.",
                sourceUrl = "https://bidar.nic.in/en/service/caste-certificate/"
            ),

            ServiceDocument(
                serviceId = "S005",
                documentId = "D020",
                status = "REQUIRED",
                notes = "The official service page lists an affidavit on Rs.20 bond paper.",
                verifiedAlternative = "",
                sourceUrl = "https://bidar.nic.in/en/service/caste-certificate/"
            ),

            ServiceDocument(
                serviceId = "S005",
                documentId = "D021",
                status = "REQUIRED",
                notes = "The official service page lists election voter ID card.",
                verifiedAlternative = "",
                sourceUrl = "https://bidar.nic.in/en/service/caste-certificate/"
            ),

            ServiceDocument(
                serviceId = "S005",
                documentId = "D022",
                status = "REQUIRED",
                notes = "The official service page lists family tree.",
                verifiedAlternative = "",
                sourceUrl = "https://bidar.nic.in/en/service/caste-certificate/"
            ),

            ServiceDocument(
                serviceId = "S005",
                documentId = "D023",
                status = "REQUIRED",
                notes = "The official service page lists residential certificate issued by revenue officers.",
                verifiedAlternative = "",
                sourceUrl = "https://bidar.nic.in/en/service/caste-certificate/"
            ),

            ServiceDocument(
                serviceId = "S006",
                documentId = "D024",
                status = "REQUIRED",
                notes = "Karnataka government guidance lists an Aadhaar-linked mobile number for the ration-card workflow.",
                verifiedAlternative = "",
                sourceUrl = "https://ahara.kar.nic.in/Home/EServices"
            ),

            ServiceDocument(
                serviceId = "S006",
                documentId = "D025",
                status = "REQUIRED",
                notes = "Address proof is used in Karnataka ration-card enrolment guidance.",
                verifiedAlternative = "",
                sourceUrl = "https://ahara.kar.nic.in/Home/EServices"
            ),

            ServiceDocument(
                serviceId = "S006",
                documentId = "D026",
                status = "REQUIRED",
                notes = "Family-member details are part of the Karnataka ration-card enrolment workflow.",
                verifiedAlternative = "",
                sourceUrl = "https://ahara.kar.nic.in/Home/EServices"
            ),

            ServiceDocument(
                serviceId = "S007",
                documentId = "D027",
                status = "REQUIRED",
                notes = "For a new permanent Driving Licence, Parivahan states that non-diplomat applicants normally need a Learner's Licence first.",
                verifiedAlternative = "",
                sourceUrl = "https://sarathi.parivahan.gov.in/"
            ),

            ServiceDocument(
                serviceId = "S007",
                documentId = "D028",
                status = "REQUIRED",
                notes = "Medical/fitness form requirements depend on age and vehicle category; Parivahan states Form 1A is required for applicants aged 40 or above and Form 1 applies for non-transport applicants under the stated workflow.",
                verifiedAlternative = "Form 1 OR Form 1A as applicable to the applicant/workflow.",
                sourceUrl = "https://parivahan.gov.in/"
            ),

            ServiceDocument(
                serviceId = "S008",
                documentId = "D029",
                status = "REQUIRED",
                notes = "Permanent-registration guidance lists Form 21 sales certificate.",
                verifiedAlternative = "",
                sourceUrl = "https://parivahan.gov.in/en/content/permanent-registration"
            ),

            ServiceDocument(
                serviceId = "S008",
                documentId = "D030",
                status = "REQUIRED",
                notes = "Permanent-registration guidance lists Form 22 roadworthiness certificate (and Form 22A from body builder where applicable).",
                verifiedAlternative = "Form 22 OR Form 22A where applicable.",
                sourceUrl = "https://parivahan.gov.in/en/content/permanent-registration"
            ),

            ServiceDocument(
                serviceId = "S008",
                documentId = "D031",
                status = "REQUIRED",
                notes = "Permanent-registration guidance lists Pollution Under Control certificate.",
                verifiedAlternative = "",
                sourceUrl = "https://parivahan.gov.in/en/content/permanent-registration"
            ),

            ServiceDocument(
                serviceId = "S008",
                documentId = "D032",
                status = "REQUIRED",
                notes = "Permanent-registration guidance lists valid vehicle insurance.",
                verifiedAlternative = "",
                sourceUrl = "https://parivahan.gov.in/en/content/permanent-registration"
            ),

            ServiceDocument(
                serviceId = "S008",
                documentId = "D037",
                status = "REQUIRED",
                notes = "Permanent-registration guidance lists proof of address.",
                verifiedAlternative = "",
                sourceUrl = "https://parivahan.gov.in/en/content/permanent-registration"
            ),

            ServiceDocument(
                serviceId = "S009",
                documentId = "D005",
                status = "REQUIRED",
                notes = "Passport Seva lists proof of date of birth as a basic supporting-document category.",
                verifiedAlternative = "",
                sourceUrl = "https://www.passportindia.gov.in/psp/GettingStarted"
            ),

            ServiceDocument(
                serviceId = "S009",
                documentId = "D033",
                status = "REQUIRED",
                notes = "Passport Seva lists proof of residence as a basic supporting-document category.",
                verifiedAlternative = "",
                sourceUrl = "https://www.passportindia.gov.in/psp/GettingStarted"
            ),

            ServiceDocument(
                serviceId = "S009",
                documentId = "D034",
                status = "REQUIRED",
                notes = "Passport Seva lists identity proof with photograph as a basic supporting-document category.",
                verifiedAlternative = "",
                sourceUrl = "https://www.passportindia.gov.in/psp/GettingStarted"
            ),

            ServiceDocument(
                serviceId = "S009",
                documentId = "D035",
                status = "REQUIRED",
                notes = "Passport Seva states that nationality is verified from supporting documents submitted.",
                verifiedAlternative = "",
                sourceUrl = "https://www.passportindia.gov.in/psp/GettingStarted"
            ),

            ServiceDocument(
                serviceId = "S010",
                documentId = "D036",
                status = "REQUIRED",
                notes = "UIDAI publishes an official acceptable-document list covering Proof of Identity, Address, Relationship and Date of Birth; the exact combination depends on the selected enrolment/update case.",
                verifiedAlternative = "Use any document category permitted by the current UIDAI acceptable-document list for the specific case.",
                sourceUrl = "https://www.uidai.gov.in/en/my-aadhaar"
            )
        )



        /*
         * ------------------------------------------------
         * VERIFIED APPLICATION PROCEDURES
         * ------------------------------------------------
         */
        val serviceProcedures = listOf(

            ServiceProcedure(
                serviceId = "S001",
                applicationProcedure = "1. Submit the birth-certificate application with the necessary documents and prescribed fee to the appropriate local registration authority.\n2. For the verified Bidar district workflow, the application is submitted to the Village Accountant (VA) and the certificate is collected from the VA.\n3. For rural Karnataka workflows, births are registered through eJanMa by Village Accountants at Nada Kacheri level; births occurring in government medical institutions/PHC/CHC sub-registration units are registered in eJanMa and certificates are issued by the institution.",
                whereToApply = "The local birth-registration authority. The verified Karnataka sources indicate the Village Accountant/Nada Kacheri route for rural registration; medical-institution births may be handled at the institution. Exact local authority depends on where the birth was registered.",
                sourceUrl = "https://bidar.nic.in/en/service/birth-certificate/",
                officialHelpline = "080-22869741 (eJanMa / Birth & Death Registration helpdesk)",
                verificationNote = "Verified from Karnataka district government sources. Local registration route can vary by location."
            ),

            ServiceProcedure(
                serviceId = "S002",
                applicationProcedure = "1. Submit the death-certificate application with the necessary documents and prescribed fee to the Village Accountant (VA).\n2. For an institutional death, provide the death certificate/details issued by the Medical Officer as specified by the official service page.\n3. For an unnatural death, provide the FIR and postmortem report as applicable.\n4. For a death at home, provide the information/documentation from the headman of the home as specified by the official service page.\n5. Collect the death certificate from the VA.",
                whereToApply = "Village Accountant (VA). The verified Bidar district source identifies all municipality offices as the location associated with this service. The exact registration authority can depend on where the death occurred.",
                sourceUrl = "https://bidar.nic.in/en/service/apply-for-death-certificates/",
                officialHelpline = "080-22869741 (eJanMa / Birth & Death Registration helpdesk)",
                verificationNote = "Verified from the current Bidar District, Government of Karnataka Death Certificate service page. Supporting requirements vary for institutional, unnatural, and home deaths."
            ),

            ServiceProcedure(
                serviceId = "S003",
                applicationProcedure = "1. Prepare the application with the necessary supporting documents.\n2. Mention the family's total income and sources in the prescribed application form.\n3. Government/private employees should attach the salary certificate as specified by the official service page.\n4. Submit the application and prescribed fee at the nearest Atalji Janasnehi Kendra (AJSK) Centre.",
                whereToApply = "Nearest AJSK Centre. The Bidar district government page also identifies all Tahsildar Offices as the location associated with this service.",
                sourceUrl = "https://bidar.nic.in/en/service/income-certificate/",
                officialHelpline = "080-22214551 (Atalji Janasnehi Kendra / AJSK helpdesk)",
                verificationNote = "Verified from the current Bidar District, Government of Karnataka Income Certificate service page. The stored workflow is a Karnataka district workflow and should be checked against the citizen's exact district."
            ),

            ServiceProcedure(
                serviceId = "S004",
                applicationProcedure = "1. Prepare the Residence Certificate application with the necessary documents and prescribed fee.\n2. Enclose residence proof: ration card, election voter ID card or school documents, as applicable.\n3. Enclose a copy of the rent agreement.\n4. Submit the application at the nearest AJSK Centre.",
                whereToApply = "Nearest AJSK Centre. The Bidar district source also identifies Tahsildar offices as the associated office location.",
                sourceUrl = "https://bidar.nic.in/en/service/residence-certificate/",
                officialHelpline = "080-22214551 (Atalji Janasnehi Kendra / AJSK helpdesk)",
                verificationNote = "Verified from the current Bidar District Government of Karnataka Residence Certificate page. This is a Bidar/Karnataka workflow and local routing should be checked for the citizen's district."
            ),

            ServiceProcedure(
                serviceId = "S005",
                applicationProcedure = "1. Prepare the prescribed Caste Certificate application.\n2. Collect the caste/parent/family/residence records applicable to the applicant's case, including educational caste evidence and other documents listed by the official service page.\n3. Submit the application with necessary documents to the competent district authority specified for the applicant's category.\n4. For SC/ST applicants, the Bidar source identifies the District Social Welfare Officer; for OBC applicants, it identifies the District Officer, Other Backward Class and Minority Welfare Department.",
                whereToApply = "Competent district welfare/revenue authority; the Bidar source identifies all Tahsildar offices as the location associated with the service and provides the category-specific district officers above.",
                sourceUrl = "https://bidar.nic.in/en/service/caste-certificate/",
                officialHelpline = "080-22214551 (Atalji Janasnehi Kendra / AJSK helpdesk)",
                verificationNote = "Verified from the current Bidar District Government of Karnataka Caste Certificate page. Several supporting documents are conditional on the applicant's family/education/employment circumstances."
            ),

            ServiceProcedure(
                serviceId = "S006",
                applicationProcedure = "1. Use the Karnataka Ahara online ration-card service for the applicable new-card or amendment workflow.\n2. Prepare the household/family information and required supporting documents.\n3. Use the authentication/contact details required by the selected workflow, including Aadhaar-linked mobile information where applicable.\n4. Submit through the Ahara service or the applicable Karnataka citizen-service channel and track the application/status.",
                whereToApply = "Karnataka Ahara online service. Karnataka district guidance also identifies Karnataka One/citizen-service channels for ration-card enrolment in applicable workflows.",
                sourceUrl = "https://ahara.kar.nic.in/Home/EServices",
                officialHelpline = "1967 / 1800-425-9339 (Karnataka Food & Civil Supplies / AharaVani)",
                verificationNote = "Verified from current Karnataka district Food & Civil Supplies sources and the Karnataka Ahara portal. Exact supporting documents vary by ration-card transaction/type."
            ),

            ServiceProcedure(
                serviceId = "S007",
                applicationProcedure = "1. Open the Sarathi Parivahan portal and select Karnataka.\n2. For a new permanent Driving Licence, complete the Learner's Licence prerequisite where applicable.\n3. Fill the applicable Driving Licence application and upload the required documents.\n4. Pay the prescribed fee and book the required appointment/slot.\n5. Visit the concerned RTO on the scheduled date with original documents and fee receipt.\n6. Complete the applicable driving test/verification process.",
                whereToApply = "Sarathi Parivahan online portal and the concerned Regional Transport Office (RTO).",
                sourceUrl = "https://sarathi.parivahan.gov.in/",
                officialHelpline = "+91-120-2459169 (Sarathi / Driving Licence technical helpdesk)",
                verificationNote = "Verified from current Parivahan/Sarathi guidance. Exact steps and documents vary by licence service, vehicle class, age and state customization."
            ),

            ServiceProcedure(
                serviceId = "S008",
                applicationProcedure = "1. For permanent registration, prepare Form 20 and the vehicle purchase/roadworthiness documents.\n2. Provide Form 21 sales certificate, Form 22 roadworthiness certificate, valid insurance, PUC and proof of address as applicable.\n3. Pay the applicable registration/tax/fee amounts.\n4. Submit through the appropriate registering authority/dealer workflow and complete vehicle verification where required.",
                whereToApply = "Registering Authority/RTO having jurisdiction over the vehicle, with online VAHAN services used where available.",
                sourceUrl = "https://parivahan.gov.in/en/content/permanent-registration",
                officialHelpline = "+91-120-4925505 (Vahan / Vehicle Services technical helpdesk)",
                verificationNote = "Verified from Parivahan permanent-registration guidance. This stored procedure covers permanent/new registration; renewal, duplicate RC, transfer and other RC services have different document sets."
            ),

            ServiceProcedure(
                serviceId = "S009",
                applicationProcedure = "1. Complete the passport application on the Passport Seva portal for the appropriate fresh or reissue category.\n2. Prepare the prescribed supporting documents and fee.\n3. Submit the application and obtain the appointment/reference details.\n4. Visit the assigned Passport Seva Kendra (PSK) or Post Office Passport Seva Kendra (POPSK) with original documents and the required self-attested copies.\n5. Complete the applicable verification/police-verification process and track the application status.",
                whereToApply = "Passport Seva Kendra (PSK) or Post Office Passport Seva Kendra (POPSK) assigned by the Passport Seva system.",
                sourceUrl = "https://www.passportindia.gov.in/psp/GettingStarted",
                officialHelpline = "1800-258-1800 (Passport Seva National Call Centre)",
                verificationNote = "Verified from current Passport Seva guidance. Exact documents depend on fresh/reissue, adult/minor, Normal/Tatkaal and other applicant-category conditions."
            ),

            ServiceProcedure(
                serviceId = "S010",
                applicationProcedure = "1. Select the appropriate Aadhaar enrolment or update service from UIDAI's My Aadhaar resources.\n2. Check the current UIDAI acceptable-document list for the exact enrolment/update case.\n3. Prepare the applicable Proof of Identity, Address, Relationship or Date of Birth documents.\n4. Book an appointment where available or visit an authorised Aadhaar Enrolment/Update Centre for the service.\n5. Complete the applicable demographic/biometric process and retain the acknowledgement/EID or update reference for tracking.",
                whereToApply = "Authorised Aadhaar Enrolment/Update Centre; UIDAI My Aadhaar provides appointment and enrolment/update resources.",
                sourceUrl = "https://uidai.gov.in/en/my-aadhaar",
                officialHelpline = "1947 (UIDAI toll-free helpline)",
                verificationNote = "Verified from current UIDAI resources. Exact documents and process vary by enrolment versus update type and the applicant's age/category."
            )
        )

        /*
         * ------------------------------------------------
         * VERIFIED COMMON PROBLEM RESOLUTION DATA
         * ------------------------------------------------
         */
        val problems = listOf(
            Problem(
                problemId = "P001",
                serviceId = "S001",
                problemType = "APPLICATION_REJECTED",
                title = "Application rejected",
                solution = "Check the rejection message or returned application details and compare the issue with the official birth-registration workflow. Do not resubmit with guessed information.",
                nextStep = "Correct the verified issue, then follow the official birth-registration channel for the next submission or clarification.",
                sourceUrl = "https://bidar.nic.in/en/service/birth-certificate/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P002",
                serviceId = "S001",
                problemType = "WRONG_INFORMATION",
                title = "Wrong information in application",
                solution = "Do not continue with incorrect birth details. Identify which birth detail is wrong and use the official birth-registration correction route.",
                nextStep = "Keep the correct supporting birth records ready and contact the competent birth-registration authority shown by the official source.",
                sourceUrl = "https://bidar.nic.in/en/service/birth-certificate/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P003",
                serviceId = "S001",
                problemType = "DUPLICATE_DOCUMENT",
                title = "Need another birth certificate copy",
                solution = "A duplicate or additional copy is a separate document request from a fresh registration. Use the official birth-certificate service to follow the applicable copy/record process.",
                nextStep = "Use the official birth-registration source for the current copy or record procedure.",
                sourceUrl = "https://bidar.nic.in/en/service/birth-certificate/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P004",
                serviceId = "S001",
                problemType = "UPDATE_CORRECTION",
                title = "Need correction/update in birth record",
                solution = "Birth-record corrections depend on the registered record and the competent registration authority. The app will not invent a correction procedure.",
                nextStep = "Use the official birth-certificate source and the competent local registration authority for the exact correction case.",
                sourceUrl = "https://bidar.nic.in/en/service/birth-certificate/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P005",
                serviceId = "S001",
                problemType = "STATUS",
                title = "Want to check birth-certificate application status",
                solution = "Use the official birth-registration service channel associated with the application. This app does not have live access to the government application record.",
                nextStep = "Open the official source and use the status/record facility available for your application.",
                sourceUrl = "https://bidar.nic.in/en/service/birth-certificate/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P006",
                serviceId = "S002",
                problemType = "APPLICATION_REJECTED",
                title = "Death-certificate application rejected",
                solution = "Check the rejection reason and compare it with the death-registration requirements for the selected death case. The app will not invent a missing reason.",
                nextStep = "Correct only the verified issue and contact the competent registration authority if the rejection reason is unclear.",
                sourceUrl = "https://bidar.nic.in/en/service/apply-for-death-certificates/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P007",
                serviceId = "S002",
                problemType = "WRONG_INFORMATION",
                title = "Wrong information in death record/application",
                solution = "Do not submit another application with guessed information. Identify the incorrect death detail and use the official correction route for the registered record.",
                nextStep = "Keep the supporting death records ready and contact the competent death-registration authority.",
                sourceUrl = "https://bidar.nic.in/en/service/apply-for-death-certificates/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P008",
                serviceId = "S002",
                problemType = "DUPLICATE_DOCUMENT",
                title = "Need another death certificate copy",
                solution = "Use the official death-registration source for the applicable copy/record request. A copy request is different from registering a new death.",
                nextStep = "Follow the official source and the competent registration authority for the record copy.",
                sourceUrl = "https://bidar.nic.in/en/service/apply-for-death-certificates/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P009",
                serviceId = "S002",
                problemType = "UPDATE_CORRECTION",
                title = "Need correction in death record",
                solution = "Death-record corrections depend on the registered record and competent authority. The app will not guess the legal correction procedure.",
                nextStep = "Use the official death-certificate source and ask the competent registration authority for the exact correction workflow.",
                sourceUrl = "https://bidar.nic.in/en/service/apply-for-death-certificates/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P010",
                serviceId = "S002",
                problemType = "STATUS",
                title = "Want to check death-certificate application status",
                solution = "This prototype does not have live access to the government registration record. Use the official service channel for the application/record status.",
                nextStep = "Open the official death-certificate source and use the available status/record facility.",
                sourceUrl = "https://bidar.nic.in/en/service/apply-for-death-certificates/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P011",
                serviceId = "S003",
                problemType = "APPLICATION_REJECTED",
                title = "Income-certificate application rejected",
                solution = "Read the rejection reason carefully. Compare it with the verified Income Certificate requirements stored in this app before preparing a corrected submission.",
                nextStep = "Correct the verified issue and resubmit through the official Karnataka/AJSK workflow when applicable.",
                sourceUrl = "https://bidar.nic.in/en/service/income-certificate/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P012",
                serviceId = "S003",
                problemType = "WRONG_INFORMATION",
                title = "Wrong information in income-certificate application",
                solution = "Do not submit incorrect family income or income-source information. Correct the information using the official application workflow.",
                nextStep = "Prepare the correct income details and supporting evidence, then use the official service procedure.",
                sourceUrl = "https://bidar.nic.in/en/service/income-certificate/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P013",
                serviceId = "S003",
                problemType = "DUPLICATE_DOCUMENT",
                title = "Need another income certificate copy",
                solution = "Use the official income-certificate service channel to determine the applicable certificate-copy or re-issue process. The app will not invent a duplicate procedure.",
                nextStep = "Use the official source or the competent revenue/AJSK channel for the exact request.",
                sourceUrl = "https://bidar.nic.in/en/service/income-certificate/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P014",
                serviceId = "S003",
                problemType = "UPDATE_CORRECTION",
                title = "Need correction in income certificate",
                solution = "If the issued certificate contains incorrect information, use the competent revenue service to ask about correction/reissue rather than submitting guessed changes.",
                nextStep = "Keep the correct supporting information and use the official Income Certificate source for the current correction route.",
                sourceUrl = "https://bidar.nic.in/en/service/income-certificate/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P015",
                serviceId = "S003",
                problemType = "STATUS",
                title = "Want to check income-certificate application status",
                solution = "This app does not have live access to the government application record. Use the official service channel for the current status.",
                nextStep = "Open the official source and use the available application/status facility.",
                sourceUrl = "https://bidar.nic.in/en/service/income-certificate/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P016",
                serviceId = "S004",
                problemType = "APPLICATION_REJECTED",
                title = "Residence-certificate application rejected",
                solution = "Check the rejection reason and compare it with the verified residence-proof and rent-agreement requirements stored in the app.",
                nextStep = "Correct the verified issue and follow the official AJSK/revenue workflow for resubmission.",
                sourceUrl = "https://bidar.nic.in/en/service/residence-certificate/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P017",
                serviceId = "S004",
                problemType = "WRONG_INFORMATION",
                title = "Wrong information in residence-certificate application",
                solution = "Correct the residence information before submitting again. Do not guess which supporting document will be accepted.",
                nextStep = "Prepare the correct residence details and use the verified Residence Certificate procedure.",
                sourceUrl = "https://bidar.nic.in/en/service/residence-certificate/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P018",
                serviceId = "S004",
                problemType = "DUPLICATE_DOCUMENT",
                title = "Need another residence certificate copy",
                solution = "Use the official Residence Certificate service to determine the applicable copy/reissue process rather than creating a new record without checking.",
                nextStep = "Use the official source and the competent revenue/AJSK channel for the exact request.",
                sourceUrl = "https://bidar.nic.in/en/service/residence-certificate/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P019",
                serviceId = "S004",
                problemType = "UPDATE_CORRECTION",
                title = "Need correction in residence certificate",
                solution = "A correction depends on the issued certificate and the competent revenue authority. The app will not invent a correction procedure.",
                nextStep = "Keep the correct residence evidence ready and follow the official correction/reissue guidance.",
                sourceUrl = "https://bidar.nic.in/en/service/residence-certificate/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P020",
                serviceId = "S004",
                problemType = "STATUS",
                title = "Want to check residence-certificate application status",
                solution = "This prototype does not have live access to the government record. Use the official service channel for current status information.",
                nextStep = "Open the official Residence Certificate source and use the available status facility.",
                sourceUrl = "https://bidar.nic.in/en/service/residence-certificate/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P021",
                serviceId = "S005",
                problemType = "APPLICATION_REJECTED",
                title = "Caste-certificate application rejected",
                solution = "Check the stated rejection reason against the verified caste-evidence records required for the selected workflow. Do not add unverified documents.",
                nextStep = "Correct the verified issue and follow the competent district/revenue/welfare workflow.",
                sourceUrl = "https://bidar.nic.in/en/service/caste-certificate/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P022",
                serviceId = "S005",
                problemType = "WRONG_INFORMATION",
                title = "Wrong information in caste-certificate application",
                solution = "Correct the applicant, family, caste or residence information before resubmission. The exact correction evidence depends on the case.",
                nextStep = "Keep the correct supporting records and use the official Caste Certificate source.",
                sourceUrl = "https://bidar.nic.in/en/service/caste-certificate/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P023",
                serviceId = "S005",
                problemType = "DUPLICATE_DOCUMENT",
                title = "Need another caste certificate copy",
                solution = "Use the official Caste Certificate service and competent authority to determine the applicable copy/reissue process.",
                nextStep = "Follow the official source rather than submitting a new certificate request without checking the existing record.",
                sourceUrl = "https://bidar.nic.in/en/service/caste-certificate/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P024",
                serviceId = "S005",
                problemType = "UPDATE_CORRECTION",
                title = "Need correction in caste certificate",
                solution = "Caste-record corrections can depend on the underlying educational, family and revenue records. The app will not guess the legal correction route.",
                nextStep = "Use the official source and competent authority for the exact correction case.",
                sourceUrl = "https://bidar.nic.in/en/service/caste-certificate/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P025",
                serviceId = "S005",
                problemType = "STATUS",
                title = "Want to check caste-certificate application status",
                solution = "This prototype does not have live access to the government record. Use the official service channel for current status information.",
                nextStep = "Open the official Caste Certificate source and use the available status/record facility.",
                sourceUrl = "https://bidar.nic.in/en/service/caste-certificate/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P026",
                serviceId = "S006",
                problemType = "APPLICATION_REJECTED",
                title = "Ration-card application rejected",
                solution = "Check the rejection reason and compare it with the verified ration-card information stored in the app. Requirements can differ by transaction type.",
                nextStep = "Correct the verified issue and use the Karnataka Ahara service for the applicable new-card/amendment workflow.",
                sourceUrl = "https://ahara.kar.nic.in/Home/EServices",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P027",
                serviceId = "S006",
                problemType = "WRONG_INFORMATION",
                title = "Wrong family/address information in ration-card application",
                solution = "Do not submit incorrect household information. Identify the incorrect member or address details and use the applicable amendment workflow.",
                nextStep = "Prepare the correct family information and use the official Ahara service.",
                sourceUrl = "https://ahara.kar.nic.in/Home/EServices",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P028",
                serviceId = "S006",
                problemType = "DUPLICATE_DOCUMENT",
                title = "Ration card lost or need another copy",
                solution = "A lost ration card is different from creating a new household record. Use the official Ahara service to identify the applicable duplicate/reprint or amendment workflow.",
                nextStep = "Open the official Ahara service and select the applicable ration-card transaction.",
                sourceUrl = "https://ahara.kar.nic.in/Home/EServices",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P029",
                serviceId = "S006",
                problemType = "UPDATE_CORRECTION",
                title = "Need to update ration-card details",
                solution = "Use the Karnataka Ahara amendment service for the applicable family/member/address change. The exact workflow depends on the type of update.",
                nextStep = "Open the official Ahara E-Services page and select the applicable amendment/update service.",
                sourceUrl = "https://ahara.kar.nic.in/Home/EServices",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P030",
                serviceId = "S006",
                problemType = "STATUS",
                title = "Want to check ration-card application status",
                solution = "This prototype does not have live access to the ration-card application record. Use the official Ahara service for current status information.",
                nextStep = "Open Ahara E-Services and use the status/tracking facility available for your transaction.",
                sourceUrl = "https://ahara.kar.nic.in/Home/EServices",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P031",
                serviceId = "S007",
                problemType = "APPLICATION_REJECTED",
                title = "Driving-licence application rejected",
                solution = "Check the rejection or return reason in the Sarathi/Parivahan workflow. The correct next step depends on the licence service and reason.",
                nextStep = "Use the official Sarathi portal to review the application and follow the applicable correction/resubmission instruction.",
                sourceUrl = "https://sarathi.parivahan.gov.in/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P032",
                serviceId = "S007",
                problemType = "WRONG_INFORMATION",
                title = "Wrong information in driving-licence application",
                solution = "Do not continue with incorrect personal or licence information. Review the application in the official Sarathi workflow and use the correction facility available for that transaction.",
                nextStep = "Open Sarathi and check the application-specific correction/resubmission option.",
                sourceUrl = "https://sarathi.parivahan.gov.in/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P033",
                serviceId = "S007",
                problemType = "RENEWAL",
                title = "Driving licence renewal",
                solution = "Renewal is a separate transport service from a new permanent licence. Use the official Sarathi/Parivahan renewal workflow for the existing licence.",
                nextStep = "Open Sarathi, select Karnataka and choose the applicable renewal service.",
                sourceUrl = "https://sarathi.parivahan.gov.in/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P034",
                serviceId = "S007",
                problemType = "DUPLICATE_DOCUMENT",
                title = "Driving licence lost or need duplicate",
                solution = "A lost or damaged driving licence should be handled through the official transport service for duplicate/replacement rather than applying as a first-time licence.",
                nextStep = "Open Sarathi and select the applicable duplicate/replacement service for the existing licence.",
                sourceUrl = "https://sarathi.parivahan.gov.in/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P035",
                serviceId = "S007",
                problemType = "UPDATE_CORRECTION",
                title = "Need correction/update in driving licence",
                solution = "Use the official transport service that corresponds to the required change. Do not treat a correction as a new-licence application.",
                nextStep = "Open Sarathi and select the applicable licence-related service for the correction/update.",
                sourceUrl = "https://sarathi.parivahan.gov.in/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P036",
                serviceId = "S007",
                problemType = "STATUS",
                title = "Want to check driving-licence application status",
                solution = "This app does not have live access to the Sarathi application record.",
                nextStep = "Open the official Sarathi portal and use the application/status facility for your transaction.",
                sourceUrl = "https://sarathi.parivahan.gov.in/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P037",
                serviceId = "S008",
                problemType = "APPLICATION_REJECTED",
                title = "Vehicle registration application rejected",
                solution = "Check the rejection reason in the Parivahan/VAHAN workflow. Registration issues can depend on the transaction and vehicle.",
                nextStep = "Correct only the verified issue and follow the official Parivahan/VAHAN workflow.",
                sourceUrl = "https://parivahan.gov.in/en/content/permanent-registration",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P038",
                serviceId = "S008",
                problemType = "WRONG_INFORMATION",
                title = "Wrong information in vehicle registration",
                solution = "Do not submit incorrect owner or vehicle information. Review the application and use the appropriate correction/update service for the registration record.",
                nextStep = "Use the official Parivahan/VAHAN service for the exact correction.",
                sourceUrl = "https://parivahan.gov.in/en/content/permanent-registration",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P039",
                serviceId = "S008",
                problemType = "DUPLICATE_DOCUMENT",
                title = "Vehicle RC lost or need duplicate",
                solution = "For a lost registration certificate, the official Parivahan guidance provides a duplicate-RC workflow. The exact supporting documents can vary by state.",
                nextStep = "Use the official Parivahan/VAHAN duplicate-RC service and follow the current instructions.",
                sourceUrl = "https://staging.parivahan.gov.in/parivahan//en/content/i-have-lost-my-registration-certificate-how-do-i-apply-duplicate-registration-certificate",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P040",
                serviceId = "S008",
                problemType = "UPDATE_CORRECTION",
                title = "Need correction/update in vehicle RC",
                solution = "The correct service depends on what needs to change, such as address or another registration entry. Do not use the permanent-registration workflow for every RC correction.",
                nextStep = "Use the official Parivahan forms/services and select the transaction matching the required change.",
                sourceUrl = "https://parivahan.gov.in/parivahan/en/content/download-forms",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P041",
                serviceId = "S008",
                problemType = "STATUS",
                title = "Want to check vehicle registration application status",
                solution = "This prototype does not have live access to the VAHAN application record.",
                nextStep = "Open the official Parivahan/VAHAN service and use the status facility available for the transaction.",
                sourceUrl = "https://parivahan.gov.in/",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P042",
                serviceId = "S009",
                problemType = "APPLICATION_REJECTED",
                title = "Passport application rejected/returned",
                solution = "Check the Passport Seva application or appointment communication for the stated reason. Passport categories have different document rules.",
                nextStep = "Use the Passport Seva guidance for the exact case before resubmitting or applying again.",
                sourceUrl = "https://www.passportindia.gov.in/psp/Apply",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P043",
                serviceId = "S009",
                problemType = "WRONG_INFORMATION",
                title = "Wrong information in passport application",
                solution = "If the application contains incorrect particulars, do not assume that a fresh application is always correct. Passport Seva distinguishes correction/reissue cases.",
                nextStep = "Review the official Passport Seva guidance and use the appropriate fresh/reissue/correction route for the case.",
                sourceUrl = "https://www.passportindia.gov.in/psp/Apply",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P044",
                serviceId = "S009",
                problemType = "RENEWAL",
                title = "Passport renewal / re-issue",
                solution = "Passport Seva treats renewal-type cases as re-issue of passport. The applicable case can include expiry, exhaustion of pages or changes in particulars.",
                nextStep = "Use the Passport Seva re-issue workflow and select the reason matching your case.",
                sourceUrl = "https://www.passportindia.gov.in/psp/docAdvisor/selectCaseReissue",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P045",
                serviceId = "S009",
                problemType = "DUPLICATE_DOCUMENT",
                title = "Passport lost or damaged",
                solution = "Lost or damaged passports are handled through the official re-issue/replacement process; the exact supporting documents depend on the case.",
                nextStep = "Open Passport Seva's re-issue/document advisor and select the lost or damaged case.",
                sourceUrl = "https://www.passportindia.gov.in/psp/docAdvisor/selectCaseReissue",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P046",
                serviceId = "S009",
                problemType = "UPDATE_CORRECTION",
                title = "Need correction/change in passport particulars",
                solution = "Passport Seva provides re-issue for changes in existing personal particulars. The exact supporting documents depend on the change.",
                nextStep = "Use the Passport Seva re-issue/document-advisor workflow for the specific change.",
                sourceUrl = "https://www.passportindia.gov.in/psp/docAdvisor/selectCaseReissue",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P047",
                serviceId = "S009",
                problemType = "STATUS",
                title = "Want to check passport application status",
                solution = "This app does not have live access to Passport Seva application records.",
                nextStep = "Use the official Passport Seva Track Application Status service with the required application details.",
                sourceUrl = "https://www.passportindia.gov.in/psp/trackApplicationService",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P048",
                serviceId = "S010",
                problemType = "APPLICATION_REJECTED",
                title = "Aadhaar enrolment/update rejected",
                solution = "Check the rejection reason provided by UIDAI and compare it with the current service/document guidance. Do not guess which document will fix the issue.",
                nextStep = "Use the official UIDAI update/enrolment guidance for the exact rejected request.",
                sourceUrl = "https://uidai.gov.in/en/my-aadhaar",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P049",
                serviceId = "S010",
                problemType = "WRONG_INFORMATION",
                title = "Wrong information in Aadhaar data",
                solution = "UIDAI supports updates to several demographic fields. Use the official update route rather than creating another Aadhaar record.",
                nextStep = "Open UIDAI's Aadhaar update service and select the field that needs correction.",
                sourceUrl = "https://uidai.gov.in/en/16-english-uk/aapka-aadhaar/27-update-your-aadhaar-data.html",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P050",
                serviceId = "S010",
                problemType = "UPDATE_CORRECTION",
                title = "Need Aadhaar update/correction",
                solution = "UIDAI provides update routes for fields including name, gender, date of birth, address and mobile number, subject to the current rules.",
                nextStep = "Use the official UIDAI update service and follow the current instructions for the field being changed.",
                sourceUrl = "https://uidai.gov.in/en/16-english-uk/aapka-aadhaar/27-update-your-aadhaar-data.html",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P051",
                serviceId = "S010",
                problemType = "DUPLICATE_DOCUMENT",
                title = "Need another Aadhaar copy",
                solution = "Do not create a second Aadhaar enrolment just because you need another copy. Use the official UIDAI/My Aadhaar resources for the applicable Aadhaar document/retrieval service.",
                nextStep = "Open My Aadhaar and use the official service applicable to your situation.",
                sourceUrl = "https://uidai.gov.in/en/my-aadhaar",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
            Problem(
                problemId = "P052",
                serviceId = "S010",
                problemType = "STATUS",
                title = "Want to check Aadhaar update/enrolment status",
                solution = "This app does not have live access to the UIDAI request record.",
                nextStep = "Use the official UIDAI/My Aadhaar status or acknowledgement facility for the request.",
                sourceUrl = "https://uidai.gov.in/en/my-aadhaar",
                verificationNote = "Stored as a conservative, source-linked problem-resolution record. The app does not invent fees, deadlines, document lists or counter details."
            ),
        )

        // Room 3 transaction
        database.withWriteTransaction {

            database.serviceDao().insertServices(services)

            database.documentDao().insertDocuments(documents)

            database.serviceDocumentDao()
                .insertServiceDocuments(serviceDocuments)

            database.serviceProcedureDao()
                .insertProcedures(serviceProcedures)
            database.problemDao()
                .insertProblems(problems)
        }
    }
}
