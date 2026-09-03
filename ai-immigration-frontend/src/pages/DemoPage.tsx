import {
  ArrowRight,
  CheckCircle,
  FileSearch,
  ShieldCheck,
  Sparkles,
  UploadCloud,
  BrainCircuit,
  Clock,
  Lock,
} from "lucide-react";

import { Link } from "react-router-dom";


export default function DemoPage() {

  const features = [
    "AI document classification",
    "Immigration requirement verification",
    "Missing document detection",
    "Personalized recommendations",
    "Secure document processing",
    "AI assistant support",
  ];


  return (

    <div className="min-h-screen bg-[#F8FAFC]">


      {/* HERO */}

      <section className="relative overflow-hidden">


        <div
          className="
            absolute
            inset-0
            bg-gradient-to-br
            from-[#E0F2FE]
            via-[#F8FAFC]
            to-[#ECFDF5]
          "
        />


        <div
          className="
            absolute
            -top-40
            -right-40
            h-96
            w-96
            rounded-full
            bg-sky-200/50
            blur-3xl
          "
        />


        <div
          className="
            absolute
            bottom-0
            -left-40
            h-96
            w-96
            rounded-full
            bg-emerald-200/40
            blur-3xl
          "
        />



        <div
          className="
            relative
            mx-auto
            max-w-7xl
            px-6
            py-28
          "
        >


          <div className="mx-auto max-w-4xl text-center">


            <div
              className="
                inline-flex
                items-center
                gap-2
                rounded-full
                border
                border-sky-200
                bg-white/80
                px-5
                py-2
                text-sm
                font-semibold
                text-slate-700
                shadow-sm
                backdrop-blur
              "
            >

              <Sparkles
                size={16}
                className="text-amber-500"
              />

              MukondoGTech AI Immigration Analysis Demo

            </div>



            <h1
              className="
                mt-8
                text-5xl
                font-black
                leading-tight
                text-slate-900
                md:text-6xl
              "
            >

              Analyze immigration documents

              <span
                className="
                  block
                  bg-gradient-to-r
                  from-sky-600
                  via-emerald-500
                  to-amber-500
                  bg-clip-text
                  text-transparent
                "
              >
                with confidence and AI precision
              </span>

            </h1>



            <p
              className="
                mx-auto
                mt-8
                max-w-3xl
                text-xl
                leading-relaxed
                text-slate-600
              "
            >
              Upload passports, visas, permits, certificates,
              and supporting documents. Our AI reviews them,
              identifies missing information, and provides
              actionable recommendations for your next steps.
            </p>



            <div
              className="
                mt-10
                flex
                flex-col
                justify-center
                gap-4
                sm:flex-row
              "
            >


              <Link
                to="/register"
                className="
                  inline-flex
                  items-center
                  justify-center
                  gap-2
                  rounded-2xl
                  bg-slate-900
                  px-8
                  py-4
                  font-bold
                  text-white
                  shadow-lg
                  transition
                  hover:-translate-y-1
                "
              >

                Start Free Analysis

                <ArrowRight size={18}/>

              </Link>



              <Link
                to="/pricing"
                className="
                  inline-flex
                  items-center
                  justify-center
                  rounded-2xl
                  border
                  border-slate-200
                  bg-white
                  px-8
                  py-4
                  font-semibold
                  text-slate-800
                  transition
                  hover:bg-sky-50
                "
              >

                View Pricing

              </Link>


            </div>


          </div>


        </div>


      </section>




      {/* STATS */}


      <section className="py-12">

        <div
          className="
            mx-auto
            grid
            max-w-6xl
            gap-6
            px-6
            md:grid-cols-3
          "
        >


          <StatCard
            icon={<BrainCircuit/>}
            title="AI Powered"
            text="Advanced document intelligence"
          />


          <StatCard
            icon={<Clock/>}
            title="Fast Analysis"
            text="Receive insights in minutes"
          />


          <StatCard
            icon={<Lock/>}
            title="Secure"
            text="Protected document processing"
          />


        </div>

      </section>






      {/* WORKFLOW */}


      <section className="py-24">


        <div className="mx-auto max-w-7xl px-6">


          <div className="text-center">


            <h2
              className="
                text-4xl
                font-black
                text-slate-900
              "
            >
              How The AI Analysis Works
            </h2>


            <p
              className="
                mx-auto
                mt-4
                max-w-2xl
                text-lg
                text-slate-600
              "
            >
              A simple three-step workflow designed
              for accurate immigration document review.
            </p>


          </div>



          <div
            className="
              mt-16
              grid
              gap-8
              md:grid-cols-3
            "
          >


            <DemoCard
              step="01"
              icon={<UploadCloud/>}
              title="Upload Documents"
              description="Securely upload passports, permits, certificates and supporting evidence."
            />


            <DemoCard
              step="02"
              icon={<FileSearch/>}
              title="AI Verification"
              description="AI extracts information, checks requirements and detects missing documents."
            />


            <DemoCard
              step="03"
              icon={<ShieldCheck/>}
              title="Get Recommendations"
              description="Receive clear results and suggested next immigration steps."
            />


          </div>


        </div>


      </section>





      {/* FEATURES */}


      <section
        className="
          bg-gradient-to-br
          from-sky-50
          to-white
          py-24
        "
      >

        <div className="mx-auto max-w-6xl px-6">


          <div className="text-center">

            <h2
              className="
                text-4xl
                font-black
                text-slate-900
              "
            >
              Powerful AI Features
            </h2>


          </div>



          <div
            className="
              mt-14
              grid
              gap-5
              md:grid-cols-2
              lg:grid-cols-3
            "
          >

            {
              features.map((feature)=>(

                <div
                  key={feature}
                  className="
                    rounded-3xl
                    border
                    border-slate-200
                    bg-white
                    p-6
                    shadow-sm
                    transition
                    hover:-translate-y-1
                    hover:shadow-xl
                  "
                >

                  <CheckCircle
                    className="mb-4 text-emerald-500"
                  />


                  <p
                    className="
                      font-semibold
                      text-slate-800
                    "
                  >
                    {feature}
                  </p>


                </div>

              ))
            }


          </div>


        </div>


      </section>






      {/* CTA */}


      <section className="px-6 pb-24">


        <div
          className="
            mx-auto
            max-w-5xl
            rounded-3xl
            bg-gradient-to-r
            from-emerald-600
            via-teal-600
            to-sky-600
            p-12
            text-center
            text-white
            shadow-xl
          "
        >


          <h2
            className="
              text-4xl
              font-black
            "
          >
            Ready to analyze your documents?
          </h2>


          <p className="mt-5 text-lg text-white/90">
            Join MukondoGTech AI and get intelligent
            immigration document insights instantly.
          </p>


          <Link
            to="/register"
            className="
              mt-8
              inline-flex
              items-center
              gap-2
              rounded-2xl
              bg-white
              px-8
              py-4
              font-bold
              text-emerald-700
              transition
              hover:-translate-y-1
            "
          >

            Start Free Analysis

            <ArrowRight size={18}/>

          </Link>


        </div>


      </section>


    </div>

  );
}




function StatCard({
  icon,
  title,
  text,
}:{
  icon:React.ReactNode;
  title:string;
  text:string;
}){

return (

<div
className="
rounded-3xl
border
bg-white
p-6
shadow-sm
"
>

<div className="text-sky-600">
{icon}
</div>

<h3 className="mt-4 font-bold text-slate-900">
{title}
</h3>

<p className="mt-2 text-slate-600">
{text}
</p>

</div>

);

}





interface DemoCardProps {

step:string;
icon:React.ReactNode;
title:string;
description:string;

}



function DemoCard({
step,
icon,
title,
description,
}:DemoCardProps){


return (

<div
className="
relative
rounded-3xl
border
border-slate-200
bg-white
p-8
shadow-sm
transition
hover:-translate-y-2
hover:shadow-xl
"
>


<span
className="
absolute
right-6
top-6
font-bold
text-amber-500
"
>
{step}
</span>



<div
className="
mb-6
flex
h-14
w-14
items-center
justify-center
rounded-2xl
bg-sky-100
text-sky-600
"
>
{icon}
</div>


<h3
className="
text-xl
font-black
text-slate-900
"
>
{title}
</h3>


<p
className="
mt-4
leading-relaxed
text-slate-600
"
>
{description}
</p>


</div>

);

}