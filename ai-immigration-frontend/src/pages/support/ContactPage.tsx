import {
  motion,
} from "framer-motion";

import {
  Mail,
  MessageSquare,
  MapPin,
  Send,
} from "lucide-react";


export default function ContactPage(){


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
              rounded-full
              bg-[#071330]
              px-5
              py-2
              text-sm
              font-semibold
              text-white
            "
          >

            MukondoGTech Support

          </div>




          <h1
            className="
              mt-8
              text-4xl
              font-black
              md:text-5xl
            "
          >

            Contact MukondoGTech AI

          </h1>




          <p
            className="
              mt-5
              text-lg
              leading-relaxed
              text-gray-600
            "
          >

            Have questions, need technical support,
            or interested in partnerships?
            Our team is ready to assist you.

          </p>


        </motion.header>







        {/* CONTENT SECTION */}


        <section
          className="
            mt-16
            grid
            gap-10
            lg:grid-cols-2
          "
        >




          {/* CONTACT INFORMATION */}


          <div
            className="
              space-y-6
            "
          >



            <ContactCard

              icon={<Mail />}

              title="Email Support"

              description="Contact our support team for account, platform, and technical assistance."

              value="support@mukondogtech.com"

            />



            <ContactCard

              icon={<MessageSquare />}

              title="Customer Support"

              description="Get help with document analysis, AI assistance, and platform usage."

              value="We respond as quickly as possible."

            />



            <ContactCard

              icon={<MapPin />}

              title="Location"

              description="MukondoGTech AI Platform"

              value="Digital AI Solutions Provider"

            />



          </div>







          {/* CONTACT FORM */}


          <motion.form

            initial={{
              opacity:0,
              x:30,
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
              space-y-5
            "

          >



            <div>

              <h2
                className="
                  text-2xl
                  font-bold
                "
              >

                Send Us A Message

              </h2>


              <p
                className="
                  mt-2
                  text-sm
                  text-gray-600
                "
              >

                Fill in the form below and our team
                will get back to you.

              </p>


            </div>





            <div>

              <label
                className="
                  mb-2
                  block
                  text-sm
                  font-semibold
                "
              >
                Full Name
              </label>


              <input

                type="text"

                placeholder="Enter your full name"

                className="
                  w-full
                  rounded-xl
                  border
                  border-[#D8DFEA]
                  px-4
                  py-3
                  outline-none
                  transition
                  focus:border-[#F4B81A]
                  focus:ring-4
                  focus:ring-[#F4B81A]/10
                "

              />

            </div>






            <div>

              <label
                className="
                  mb-2
                  block
                  text-sm
                  font-semibold
                "
              >
                Email Address
              </label>


              <input

                type="email"

                placeholder="Enter your email address"

                className="
                  w-full
                  rounded-xl
                  border
                  border-[#D8DFEA]
                  px-4
                  py-3
                  outline-none
                  transition
                  focus:border-[#F4B81A]
                  focus:ring-4
                  focus:ring-[#F4B81A]/10
                "

              />

            </div>







            <div>

              <label
                className="
                  mb-2
                  block
                  text-sm
                  font-semibold
                "
              >
                Subject
              </label>


              <input

                type="text"

                placeholder="How can we help?"

                className="
                  w-full
                  rounded-xl
                  border
                  border-[#D8DFEA]
                  px-4
                  py-3
                  outline-none
                  transition
                  focus:border-[#F4B81A]
                  focus:ring-4
                  focus:ring-[#F4B81A]/10
                "

              />

            </div>







            <div>

              <label
                className="
                  mb-2
                  block
                  text-sm
                  font-semibold
                "
              >
                Message
              </label>


              <textarea

                rows={5}

                placeholder="Write your message..."

                className="
                  w-full
                  resize-none
                  rounded-xl
                  border
                  border-[#D8DFEA]
                  px-4
                  py-3
                  outline-none
                  transition
                  focus:border-[#F4B81A]
                  focus:ring-4
                  focus:ring-[#F4B81A]/10
                "

              />


            </div>







            <button

              type="submit"

              className="
                flex
                w-full
                items-center
                justify-center
                gap-2
                rounded-xl
                bg-[#071330]
                py-4
                font-bold
                text-white
                transition
                hover:bg-[#183B6B]
              "

            >

              <Send size={18}/>

              Send Message

            </button>




          </motion.form>



        </section>



      </div>


    </main>

  );

}







interface ContactCardProps {

  icon: React.ReactNode;

  title:string;

  description:string;

  value:string;

}



function ContactCard({

  icon,

  title,

  description,

  value,

}:ContactCardProps){


  return (

    <motion.div

      whileHover={{
        y:-5,
      }}

      className="
        rounded-3xl
        border
        border-[#E5E7EB]
        bg-white
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
          bg-[#FFF4CC]
          text-[#F4B81A]
        "
      >

        {icon}

      </div>




      <h3
        className="
          mt-5
          text-lg
          font-bold
        "
      >

        {title}

      </h3>



      <p
        className="
          mt-2
          text-sm
          leading-relaxed
          text-gray-600
        "
      >

        {description}

      </p>




      <p
        className="
          mt-4
          font-semibold
          text-[#183B6B]
        "
      >

        {value}

      </p>


    </motion.div>

  );

}