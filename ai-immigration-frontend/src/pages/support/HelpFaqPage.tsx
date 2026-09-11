import {
  motion,
} from "framer-motion";

import {
  useState,
} from "react";

import {
  ChevronDown,
  HelpCircle,
  ShieldCheck,
  FileText,
  CreditCard,
  BrainCircuit,
} from "lucide-react";


const faqItems = [

  {
    question:
      "What is MukondoGTech AI Platform?",

    answer:
      "MukondoGTech AI Platform is an AI-powered document analysis platform designed to help users analyse immigration documents, organize applications, and receive intelligent assistance.",
  },


  {
    question:
      "Does the AI guarantee immigration approval?",

    answer:
      "No. The platform provides AI-powered assistance and document insights but does not guarantee visa approval or replace official immigration authorities or professional legal advice.",
  },


  {
    question:
      "What documents can I upload?",

    answer:
      "Users can upload supported immigration-related documents including passports, application forms, certificates, and supporting evidence for AI analysis.",
  },


  {
    question:
      "Are my documents secure?",

    answer:
      "Yes. MukondoGTech applies security controls including authentication, access protection, and secure cloud storage practices to protect user information.",
  },


  {
    question:
      "How does billing work?",

    answer:
      "Users can subscribe to available plans and payments are processed through secure payment providers. Subscription details can be managed from the billing dashboard.",
  },


  {
    question:
      "Can I cancel my subscription?",

    answer:
      "Yes. Users can manage or cancel subscriptions from their account billing settings.",
  },


  {
    question:
      "Can businesses use MukondoGTech?",

    answer:
      "Yes. Organizations and immigration consultants can use the platform to improve document workflows and client management.",
  },

];



const categories = [

  {
    icon: BrainCircuit,
    title:"AI Assistance",
    description:
      "Understand how our AI analyses immigration documents.",
  },


  {
    icon: FileText,
    title:"Documents",
    description:
      "Learn about uploads, storage, and document processing.",
  },


  {
    icon: CreditCard,
    title:"Billing",
    description:
      "Manage subscriptions and payment questions.",
  },


  {
    icon: ShieldCheck,
    title:"Security",
    description:
      "Understand how we protect your information.",
  },

];



export default function HelpFaqPage(){

  const [
    active,
    setActive,
  ] = useState<number | null>(null);



  return (

    <main
      className="
        min-h-screen
        px-6
        pt-32
        pb-20
        text-slate-100
      "
    >


      <div
        className="
          mx-auto
          max-w-6xl
        "
      >



        {/* HERO */}


        <motion.header

          initial={{
            opacity:0,
            y:30,
          }}

          animate={{
            opacity:1,
            y:0,
          }}

          transition={{
            duration:0.5,
          }}

          className="
            mx-auto
            max-w-3xl
            text-center
          "

        >


          <div
            className="
              inline-flex
              items-center
              gap-2
              rounded-full
              bg-[#071426]
              px-5
              py-2
              text-sm
              font-semibold
              text-white
            "
          >

            <HelpCircle
              size={16}
              className="text-[#C6A15B]"
            />

            Help Center

          </div>



          <h1
            className="
              mt-8
              text-4xl
              font-black
              md:text-5xl
            "
          >

            Help & Frequently Asked Questions

          </h1>



          <p
            className="
              mt-5
              text-lg
              leading-relaxed
              text-slate-300
            "
          >

            Find answers about MukondoGTech AI Platform,
            document processing, security, subscriptions,
            and AI-powered immigration assistance.

          </p>


        </motion.header>






        {/* CATEGORY CARDS */}


        <section
          className="
            mt-16
            grid
            gap-6
            sm:grid-cols-2
            lg:grid-cols-4
          "
        >


          {
            categories.map(
              (item)=>{

                const Icon=item.icon;


                return (

                  <motion.div

                    key={item.title}

                    whileHover={{
                      y:-5,
                    }}

                    className="
                      rounded-3xl
                      border
                      border-white/10
                      bg-white/5
                      backdrop-blur-xl
                      p-6
                      shadow-sm
                      transition
                    "

                  >


                    <div
                      className="
                        flex
                        h-12
                        w-12
                        items-center
                        justify-center
                        rounded-2xl
                        bg-[#C6A15B]/15
                      "
                    >

                      <Icon
                        size={24}
                        className="text-[#C6A15B]"
                      />

                    </div>



                    <h3
                      className="
                        mt-5
                        font-bold
                        text-lg
                        text-white
                      "
                    >

                      {item.title}

                    </h3>



                    <p
                      className="
                        mt-2
                        text-sm
                        leading-relaxed
                        text-slate-300
                      "
                    >

                      {item.description}

                    </p>


                  </motion.div>

                );

              }
            )
          }


        </section>







        {/* FAQ SECTION */}


        <section
          className="
            mt-16
            space-y-4
          "
        >


          {
            faqItems.map(
              (item,index)=>(


                <motion.div

                  key={item.question}

                  initial={{
                    opacity:0,
                    y:15,
                  }}

                  whileInView={{
                    opacity:1,
                    y:0,
                  }}

                  viewport={{
                    once:true,
                  }}

                  transition={{
                    duration:0.3,
                  }}

                  className="
                    overflow-hidden
                    rounded-3xl
                    border
                    border-white/10
                    bg-white/5
                    backdrop-blur-xl
                  "

                >



                  <button

                    onClick={() =>
                      setActive(
                        active === index
                        ? null
                        : index
                      )
                    }

                    className="
                      flex
                      w-full
                      items-center
                      justify-between
                      gap-5
                      p-6
                      text-left
                      font-semibold
                      text-white
                    "

                  >

                    <span>
                      {item.question}
                    </span>


                    <ChevronDown

                      size={20}

                      className={`
                        flex-shrink-0
                        transition-transform
                        ${
                          active === index
                          ? "rotate-180"
                          : ""
                        }
                      `}

                    />


                  </button>




                  <motion.div

                    initial={false}

                    animate={{
                      height:
                        active === index
                        ? "auto"
                        : 0,

                      opacity:
                        active === index
                        ? 1
                        : 0,
                    }}

                    className="
                      overflow-hidden
                    "

                  >

                    <p
                      className="
                        px-6
                        pb-6
                        leading-relaxed
                        text-slate-300
                      "
                    >

                      {item.answer}

                    </p>


                  </motion.div>


                </motion.div>


              )
            )
          }


        </section>



      </div>


    </main>

  );

}