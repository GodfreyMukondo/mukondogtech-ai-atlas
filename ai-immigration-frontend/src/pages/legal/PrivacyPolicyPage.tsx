import {
  motion,
} from "framer-motion";

import {
  ArrowLeft,
  Database,
  Lock,
  Eye,
  FileText,
  UserRound,
  ShieldCheck,
} from "lucide-react";

import {
  Link,
} from "react-router-dom";


const sections = [

{
icon: Database,
title:"1. Information We Collect",

content:`
We collect information necessary to provide and improve MukondoGTech AI
Platform.

This may include:

• Name and account information.
• Email address.
• Uploaded documents.
• Usage activity.
• Technical information such as browser and device details.
`
},


{
icon: FileText,
title:"2. Document Processing",

content:`
Documents uploaded to our platform are processed securely for AI-powered
analysis.

We use uploaded documents only to provide requested services, improve
platform functionality, maintain security, and comply with legal
requirements.

We do not sell user documents or personal information.
`
},


{
icon: Lock,
title:"3. Data Security",

content:`
We implement security measures including:

• Encryption where appropriate.
• Authentication controls.
• Access restrictions.
• Secure cloud storage practices.

However, no internet-based system can guarantee absolute security.
`
},


{
icon: Eye,
title:"4. How We Use Information",

content:`
We may use collected information to:

• Provide platform features.
• Analyse documents.
• Manage accounts.
• Process payments.
• Improve AI services.
• Prevent fraud and abuse.
`
},


{
icon: UserRound,
title:"5. User Rights",

content:`
Users may request:

• Access to personal information.
• Correction of inaccurate information.
• Account deletion.
• Removal of uploaded documents where applicable.
`
},


{
icon: ShieldCheck,
title:"6. Third-Party Services",

content:`
We may use trusted third-party providers for:

• Cloud hosting.
• Payment processing.
• Email communication.
• AI infrastructure.

These providers process information according to their own privacy
policies and security practices.
`
},


{
icon: Database,
title:"7. Data Retention",

content:`
We retain information only for as long as necessary to provide services,
meet legal obligations, resolve disputes, and maintain security.
`
},


{
icon: ShieldCheck,
title:"8. Children's Privacy",

content:`
MukondoGTech AI Platform is not intended for children under the applicable
minimum legal age.

We do not knowingly collect personal information from children.
`
},


{
icon: FileText,
title:"9. Changes to This Privacy Policy",

content:`
We may update this Privacy Policy from time to time.

The latest version will always be available on this page with the updated
effective date.
`
},


{
icon: UserRound,
title:"10. Contact Us",

content:`
For privacy-related questions:

MukondoGTech AI Platform

Email:
privacy@mukondogtech.com
`
}

];


export default function PrivacyPolicyPage(){

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
mb-10
text-sm
font-medium
hover:text-[#C6A15B]
"
>

<ArrowLeft size={18}/>

Back Home

</Link>



<motion.header

initial={{
opacity:0,
y:20
}}

animate={{
opacity:1,
y:0
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

Privacy Policy

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

This Privacy Policy explains how MukondoGTech AI Platform collects,
uses, protects, and manages your information.

</p>


</motion.header>



<div
className="
space-y-8
"
>


{
sections.map(
({
icon:Icon,
title,
content
})=>(

<motion.section

key={title}

initial={{
opacity:0,
y:20
}}

whileInView={{
opacity:1,
y:0
}}

viewport={{
once:true
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
gap-3
items-center
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
)

}


</div>


</div>


</div>

);

}