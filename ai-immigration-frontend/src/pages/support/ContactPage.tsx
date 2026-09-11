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
              bg-[#071426]
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
              text-slate-300
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
              border-white/10
              bg-white/5
              backdrop-blur-xl
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
                  text-slate-300
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
                  border-white/15
                  bg-white/5
                  text-white
                  placeholder:text-slate-500
                  px-4
                  py-3
                  outline-none
                  transition
                  focus:border-[#C6A15B]
                  focus:ring-4
                  focus:ring-[#C6A15B]/10
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
                  border-white/15
                  bg-white/5
                  text-white
                  placeholder:text-slate-500
                  px-4
                  py-3
                  outline-none
                  transition
                  focus:border-[#C6A15B]
                  focus:ring-4
                  focus:ring-[#C6A15B]/10
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
                  border-white/15
                  bg-white/5
                  text-white
                  placeholder:text-slate-500
                  px-4
                  py-3
                  outline-none
                  transition
                  focus:border-[#C6A15B]
                  focus:ring-4
                  focus:ring-[#C6A15B]/10
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
                  border-white/15
                  bg-white/5
                  text-white
                  placeholder:text-slate-500
                  px-4
                  py-3
                  outline-none
                  transition
                  focus:border-[#C6A15B]
                  focus:ring-4
                  focus:ring-[#C6A15B]/10
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
                bg-[#071426]
                py-4
                font-bold
                text-white
                transition
                hover:bg-[#3C4C61]
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
          text-[#C6A15B]
        "
      >

        {icon}

      </div>




      <h3
        className="
          mt-5
          text-lg
          font-bold
          text-white
        "
      >

        {title}

      </h3>



      <p
        className="
          mt-2
          text-sm
          leading-relaxed
          text-slate-300
        "
      >

        {description}

      </p>




      <p
        className="
          mt-4
          font-semibold
          text-[#C6A15B]
        "
      >

        {value}

      </p>


    </motion.div>

  );

}