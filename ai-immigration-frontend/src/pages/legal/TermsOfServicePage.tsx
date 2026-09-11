import {
  motion,
} from "framer-motion";

import {
  ArrowLeft,
  FileText,
  ShieldCheck,
  UserCheck,
  CreditCard,
  AlertTriangle,
  Scale,
} from "lucide-react";

import {
  Link,
} from "react-router-dom";


const sections = [
  {
    icon: UserCheck,
    title: "1. Acceptance of Terms",
    content: `
By accessing or using MukondoGTech AI Platform, you agree to be bound by
these Terms of Service. If you do not agree with any part of these terms,
you must not use our services.

These terms apply to all users, including visitors, registered users,
subscribers, and organizations using our platform.
`,
  },

  {
    icon: FileText,
    title: "2. Description of Service",
    content: `
MukondoGTech AI Platform provides artificial intelligence-powered document
analysis, immigration assistance tools, document management, and related
digital services.

Our AI tools are designed to assist users by analysing information and
providing guidance. The platform does not replace professional immigration
lawyers, government authorities, or official immigration decisions.
`,
  },

  {
    icon: ShieldCheck,
    title: "3. User Accounts and Security",
    content: `
Users are responsible for maintaining the confidentiality of their account
credentials.

You agree to:

• Provide accurate registration information.
• Protect your password and authentication details.
• Immediately notify us of unauthorized account access.
• Use the platform only for lawful purposes.
`,
  },

  {
    icon: FileText,
    title: "4. Document Uploads and User Content",
    content: `
Users may upload documents required for AI analysis and processing.

You retain ownership of all documents and information you upload.

By uploading content, you grant MukondoGTech permission to securely process
your documents only for providing platform services.

You must not upload illegal, harmful, fraudulent, or unauthorized content.
`,
  },

  {
    icon: CreditCard,
    title: "5. Payments and Subscriptions",
    content: `
Some features may require paid subscriptions.

Subscription fees, billing periods, and available plans will be displayed
before purchase.

Payments are processed through approved payment providers. Users are
responsible for maintaining accurate billing information.
`,
  },

  {
    icon: AlertTriangle,
    title: "6. AI Limitations",
    content: `
Artificial intelligence systems may occasionally produce inaccurate,
incomplete, or outdated information.

MukondoGTech does not guarantee that AI-generated recommendations will
result in immigration approval or any specific outcome.

Users should verify important decisions with official sources or qualified
professionals.
`,
  },

  {
    icon: Scale,
    title: "7. Prohibited Activities",
    content: `
Users must not:

• Attempt unauthorized access to the platform.
• Reverse engineer or copy platform technology.
• Upload malicious files.
• Use the service for fraudulent activities.
• Interfere with system security or availability.
`,
  },

  {
    icon: Scale,
    title: "8. Limitation of Liability",
    content: `
To the maximum extent permitted by law, MukondoGTech is not responsible for
losses resulting from reliance on AI-generated information, service
interruptions, unauthorized access, or third-party services.
`,
  },

  {
    icon: FileText,
    title: "9. Changes to These Terms",
    content: `
We may update these Terms of Service periodically.

Updated terms will be published on this page with a revised effective date.
Continued use of the platform after changes means you accept the updated
terms.
`,
  },

  {
    icon: ShieldCheck,
    title: "10. Contact Information",
    content: `
If you have questions about these Terms of Service, please contact:

MukondoGTech AI Platform

Email:
support@mukondogtech.com
`,
  },
];


export default function TermsOfServicePage() {

  return (
    <div
      className="
        min-h-screen
        text-slate-100
        py-16
        px-6
      "
    >

      <div
        className="
          max-w-5xl
          mx-auto
        "
      >

        <Link
          to="/"
          className="
            inline-flex
            items-center
            gap-2
            text-sm
            font-medium
            mb-10
            hover:text-[#C6A15B]
            transition
          "
        >
          <ArrowLeft size={18}/>
          Back Home
        </Link>


        <motion.header
          initial={{
            opacity:0,
            y:20,
          }}

          animate={{
            opacity:1,
            y:0,
          }}

          className="
            mb-12
          "
        >

          <h1
            className="
              text-4xl
              md:text-5xl
              font-bold
            "
          >
            Terms of Service
          </h1>


          <p
            className="
              mt-4
              text-slate-300
            "
          >
            Last updated: July 25, 2026
          </p>


          <p
            className="
              mt-6
              text-lg
              leading-relaxed
            "
          >
            These Terms explain the rules and conditions for using
            MukondoGTech AI Platform services.
          </p>

        </motion.header>



        <div
          className="
            space-y-8
          "
        >

          {sections.map(
            ({
              icon:Icon,
              title,
              content,
            })=>(

              <motion.section

                key={title}

                initial={{
                  opacity:0,
                  y:20,
                }}

                whileInView={{
                  opacity:1,
                  y:0,
                }}

                viewport={{
                  once:true,
                }}

                className="
                  bg-white/5
                  backdrop-blur-xl
                  rounded-2xl
                  shadow-sm
                  p-8
                  border
                  border-white/10
                "
              >

                <div
                  className="
                    flex
                    items-center
                    gap-3
                    mb-4
                  "
                >

                  <Icon
                    className="
                      text-[#C6A15B]
                    "
                  />

                  <h2
                    className="
                      text-xl
                      font-semibold
                    "
                  >
                    {title}
                  </h2>

                </div>


                <p
                  className="
                    whitespace-pre-line
                    leading-relaxed
                    text-slate-300
                  "
                >
                  {content}
                </p>


              </motion.section>

            )
          )}

        </div>


      </div>

    </div>
  );
}