import { motion } from "framer-motion";
import {
  ShieldCheck,
  LockKeyhole,
  FileLock2,
  BrainCircuit,
  Cloud,
  UserCheck,
} from "lucide-react";


const securityFeatures = [
  {
    icon: LockKeyhole,
    title: "Secure Authentication",
    description:
      "Protected user accounts with secure authentication workflows designed to safeguard access.",
  },
  {
    icon: FileLock2,
    title: "Protected Document Processing",
    description:
      "Immigration documents are handled through controlled workflows designed around privacy and security.",
  },
  {
    icon: BrainCircuit,
    title: "Responsible AI Analysis",
    description:
      "AI helps analyze documents and provide insights while maintaining transparency throughout the process.",
  },
  {
    icon: Cloud,
    title: "Reliable Cloud Infrastructure",
    description:
      "A scalable cloud platform designed for secure access, reliability, and consistent performance.",
  },
  {
    icon: UserCheck,
    title: "Controlled Access",
    description:
      "Access controls help protect sensitive immigration information and user data.",
  },
  {
    icon: ShieldCheck,
    title: "Privacy-First Approach",
    description:
      "We prioritize responsible data handling throughout every stage of the immigration workflow.",
  },
];


export default function SecurityPage() {

  return (

    <main
      className="
        min-h-screen
      "
    >


      {/* Hero Section */}

      <section
        className="
          relative
          overflow-hidden
          px-6
          py-24
        "
      >

        {/* Background Glow */}

        <div
          aria-hidden="true"
          className="
            absolute
            left-0
            top-0
            h-72
            w-72
            rounded-full
            bg-[#C6A15B]/10
            blur-3xl
          "
        />


        <div
          className="
            relative
            mx-auto
            max-w-5xl
            text-center
          "
        >

          <motion.div
            initial={{
              opacity:0,
              scale:.8,
            }}
            animate={{
              opacity:1,
              scale:1,
            }}
            transition={{
              duration:.5,
            }}
            className="
              mx-auto
              flex
              h-20
              w-20
              items-center
              justify-center
              rounded-full
              bg-[#C6A15B]/15
              shadow-lg
            "
          >

            <ShieldCheck
              size={38}
              className="text-[#C6A15B]"
            />

          </motion.div>




          <h1
            className="
              mt-8
              text-4xl
              font-bold
              leading-tight
              text-white
              md:text-6xl
            "
          >

            Security Built Into
            Every Immigration Workflow

          </h1>



          <p
            className="
              mx-auto
              mt-6
              max-w-3xl
              text-lg
              leading-relaxed
              text-slate-300
              md:text-xl
            "
          >

            MukondoGTech AI combines secure technology,
            privacy-focused practices, and responsible AI
            to help protect sensitive immigration data.

          </p>


        </div>


      </section>






      {/* Security Features */}

      <section
        className="
          px-6
          pb-24
        "
      >

        <div
          className="
            mx-auto
            grid
            max-w-7xl
            gap-8
            md:grid-cols-2
            lg:grid-cols-3
          "
        >

          {securityFeatures.map(
            (feature,index)=>{

              const Icon = feature.icon;


              return (

                <motion.article

                  key={feature.title}

                  initial={{
                    opacity:0,
                    y:30,
                  }}

                  whileInView={{
                    opacity:1,
                    y:0,
                  }}

                  viewport={{
                    once:true,
                  }}

                  transition={{
                    duration:.5,
                    delay:index * .1,
                  }}

                  className="
                    rounded-3xl
                    border
                    border-white/10
                    bg-white/5
                    backdrop-blur-xl
                    p-8
                    shadow-lg
                    transition-all
                    duration-300
                    hover:-translate-y-2
                    hover:shadow-xl
                  "
                >


                  <div
                    className="
                      mb-6
                      flex
                      h-14
                      w-14
                      items-center
                      justify-center
                      rounded-2xl
                      bg-[#C6A15B]/15
                    "
                  >

                    <Icon
                      size={28}
                      className="text-[#C6A15B]"
                    />

                  </div>



                  <h2
                    className="
                      mb-3
                      text-xl
                      font-semibold
                      text-white
                    "
                  >
                    {feature.title}
                  </h2>



                  <p
                    className="
                      leading-relaxed
                      text-slate-300
                    "
                  >
                    {feature.description}
                  </p>


                </motion.article>

              );

            }
          )}

        </div>

      </section>







      {/* Security Promise */}

      <section
        className="
          relative
          overflow-hidden
          bg-[#C6A15B]/10
          backdrop-blur-xl
          px-6
          py-24
        "
      >

        {/* Decorative Gold Glow */}

        <div
          aria-hidden="true"
          className="
            absolute
            right-0
            top-0
            h-80
            w-80
            rounded-full
            bg-[#C6A15B]/20
            blur-3xl
          "
        />


        <div
          className="
            relative
            mx-auto
            max-w-4xl
            text-center
          "
        >


          <motion.div

            initial={{
              opacity:0,
              y:30,
            }}

            whileInView={{
              opacity:1,
              y:0,
            }}

            viewport={{
              once:true,
            }}

            transition={{
              duration:.6,
            }}

          >


            <h2
              className="
                text-3xl
                font-bold
                leading-tight
                text-white
                md:text-4xl
              "
            >

              Protecting your immigration journey
              with intelligent technology

            </h2>



            <p
              className="
                mx-auto
                mt-6
                max-w-3xl
                text-lg
                leading-relaxed
                text-slate-300
              "
            >

              From document upload to AI analysis,
              MukondoGTech AI focuses on creating a
              secure, transparent, and reliable
              experience for every user.

            </p>


          </motion.div>


        </div>


      </section>


    </main>

  );
}