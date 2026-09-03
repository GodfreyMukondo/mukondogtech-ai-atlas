import {
  motion,
} from "framer-motion";

import {
  BrainCircuit,
  ShieldCheck,
  Globe,
  Rocket,
} from "lucide-react";


const values = [

  {
    icon: BrainCircuit,
    title: "Artificial Intelligence",
    text:
      "We use modern AI technologies to simplify complex document analysis and deliver intelligent insights.",
  },

  {
    icon: ShieldCheck,
    title: "Security First",
    text:
      "We prioritize privacy, security, and responsible data handling throughout our platform.",
  },

  {
    icon: Globe,
    title: "Global Access",
    text:
      "We create technology that helps users access reliable immigration information worldwide.",
  },

  {
    icon: Rocket,
    title: "Innovation",
    text:
      "We continuously improve our platform through modern software engineering and AI innovation.",
  },

];



export default function AboutPage() {


  return (

    <main
      className="
        min-h-screen
        bg-[#F8F6F1]
        px-6
        pt-32
        pb-20
        text-[#0B1736]
      "
    >


      <div
        className="
          mx-auto
          max-w-6xl
        "
      >



        {/* HERO SECTION */}

        <motion.section

          initial={{
            opacity:0,
            y:30,
          }}

          animate={{
            opacity:1,
            y:0,
          }}

          transition={{
            duration:0.6,
          }}

          className="
            mx-auto
            max-w-4xl
            text-center
          "

        >


          <div
            className="
              inline-flex
              items-center
              rounded-full
              bg-[#071330]
              px-5
              py-2
              text-sm
              font-semibold
              text-white
            "
          >

            MukondoGTech AI Platform

          </div>



          <h1
            className="
              mt-8
              text-4xl
              font-black
              leading-tight
              md:text-6xl
            "
          >

            Building the Future with

            <span
              className="
                block
                text-[#F4B81A]
              "
            >
              Artificial Intelligence
            </span>

          </h1>



          <p
            className="
              mx-auto
              mt-6
              max-w-3xl
              text-lg
              leading-relaxed
              text-gray-600
            "
          >

            MukondoGTech AI Platform is an intelligent software ecosystem
            designed to help individuals and organizations analyse documents,
            improve workflows, and make better decisions using secure AI
            technologies.

          </p>


        </motion.section>





        {/* MISSION AND VISION */}


        <section
          className="
            mt-20
            grid
            gap-8
            md:grid-cols-2
          "
        >


          <motion.div

            initial={{
              opacity:0,
              x:-20,
            }}

            whileInView={{
              opacity:1,
              x:0,
            }}

            viewport={{
              once:true,
            }}

            className="
              rounded-3xl
              border
              border-[#E5E7EB]
              bg-white
              p-8
              shadow-sm
            "

          >


            <h2
              className="
                text-2xl
                font-bold
              "
            >

              Our Mission

            </h2>


            <p
              className="
                mt-4
                leading-relaxed
                text-gray-600
              "
            >

              To build accessible AI solutions that simplify complex
              processes and empower people through intelligent technology.

            </p>


          </motion.div>





          <motion.div

            initial={{
              opacity:0,
              x:20,
            }}

            whileInView={{
              opacity:1,
              x:0,
            }}

            viewport={{
              once:true,
            }}

            className="
              rounded-3xl
              border
              border-[#E5E7EB]
              bg-white
              p-8
              shadow-sm
            "

          >


            <h2
              className="
                text-2xl
                font-bold
              "
            >

              Our Vision

            </h2>


            <p
              className="
                mt-4
                leading-relaxed
                text-gray-600
              "
            >

              To become a trusted AI technology provider delivering secure,
              innovative, and practical digital solutions for businesses
              and individuals.

            </p>


          </motion.div>


        </section>






        {/* VALUES SECTION */}


        <section
          className="
            mt-20
          "
        >


          <div
            className="
              mb-10
              text-center
            "
          >


            <h2
              className="
                text-3xl
                font-black
              "
            >

              Our Core Values

            </h2>


            <p
              className="
                mt-3
                text-gray-600
              "
            >

              The principles that guide our technology and product decisions.

            </p>


          </div>





          <div
            className="
              grid
              gap-6
              sm:grid-cols-2
              lg:grid-cols-4
            "
          >


            {
              values.map(
                (value)=>{


                  const Icon = value.icon;


                  return (

                    <motion.div

                      key={value.title}

                      whileHover={{
                        y:-6,
                      }}

                      transition={{
                        duration:0.2,
                      }}

                      className="
                        rounded-3xl
                        border
                        border-[#E5E7EB]
                        bg-white
                        p-6
                        shadow-sm
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
                          bg-[#FFF4CC]
                        "
                      >

                        <Icon
                          size={24}
                          className="
                            text-[#F4B81A]
                          "
                        />

                      </div>




                      <h3
                        className="
                          mt-5
                          font-bold
                          text-lg
                        "
                      >

                        {value.title}

                      </h3>




                      <p
                        className="
                          mt-3
                          text-sm
                          leading-relaxed
                          text-gray-600
                        "
                      >

                        {value.text}

                      </p>


                    </motion.div>

                  );


                }
              )
            }


          </div>


        </section>



      </div>


    </main>

  );

}