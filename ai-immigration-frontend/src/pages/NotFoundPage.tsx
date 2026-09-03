import {
  Link,
  useNavigate,
} from "react-router-dom";

import {
  ArrowLeft,
  Home,
  SearchX,
  ScanSearch,
} from "lucide-react";

import {
  motion,
} from "framer-motion";



export default function NotFoundPage() {


  const navigate = useNavigate();



  return (

    <main
      className="
        min-h-screen
        bg-[#F8F6F1]
        flex
        items-center
        justify-center
        px-6
      "
    >

      <motion.div
        initial={{
          opacity: 0,
          y: 30,
        }}

        animate={{
          opacity: 1,
          y: 0,
        }}

        transition={{
          duration: 0.5,
        }}

        className="
          w-full
          max-w-xl
          text-center
        "
      >


        {/* Logo */}

        <Link
          to="/"
          className="
            flex
            justify-center
            items-center
            gap-3
            mb-10
          "
        >

          <div
            className="
              w-14
              h-14
              rounded-2xl
              border
              border-[#F4B81A]
              flex
              items-center
              justify-center
              bg-white
            "
          >

            <ScanSearch
              size={30}
              className="
                text-[#F4B81A]
              "
            />

          </div>



          <h1
            className="
              text-3xl
              font-bold
              text-[#0B1736]
            "
          >

            MukondoGTech

            <span
              className="
                text-[#F4B81A]
              "
            >
              AI
            </span>

          </h1>


        </Link>





        {/* Card */}

        <div
          className="
            bg-white
            border
            border-[#E5DED1]
            rounded-3xl
            shadow-sm
            p-10
          "
        >


          <motion.div

            animate={{
              rotate: [
                0,
                -8,
                8,
                0,
              ],
            }}

            transition={{
              duration: 2,
              repeat: Infinity,
              repeatDelay: 2,
            }}

            className="
              flex
              justify-center
              mb-6
            "
          >

            <div
              className="
                w-24
                h-24
                rounded-full
                bg-[#F8F6F1]
                flex
                items-center
                justify-center
              "
            >

              <SearchX
                size={48}
                className="
                  text-[#F4B81A]
                "
              />

            </div>

          </motion.div>





          <h2
            className="
              text-7xl
              font-extrabold
              text-[#0B1736]
              tracking-tight
            "
          >

            404

          </h2>





          <h3
            className="
              mt-4
              text-2xl
              font-bold
              text-[#0B1736]
            "
          >

            Page Not Found

          </h3>





          <p
            className="
              mt-4
              text-[#7D8CA3]
              leading-relaxed
            "
          >

            Sorry, the page you are looking for
            does not exist or may have been moved.
            Return to the dashboard or continue
            exploring MukondoGTech AI.

          </p>





          {/* Actions */}

          <div
            className="
              mt-8
              flex
              flex-col
              sm:flex-row
              gap-4
              justify-center
            "
          >


            <button
              onClick={() => navigate(-1)}
              className="
                flex
                items-center
                justify-center
                gap-2
                px-6
                py-3
                rounded-xl
                border
                border-[#E5DED1]
                text-[#0B1736]
                font-semibold
                hover:bg-[#F8F6F1]
                transition
              "
            >

              <ArrowLeft
                size={18}
              />

              Go Back

            </button>





            <Link
              to="/"
              className="
                flex
                items-center
                justify-center
                gap-2
                px-6
                py-3
                rounded-xl
                bg-[#0B1736]
                text-white
                font-semibold
                hover:bg-[#152650]
                transition
              "
            >

              <Home
                size={18}
              />

              Back Home

            </Link>


          </div>


        </div>




        <p
          className="
            mt-8
            text-sm
            text-[#7D8CA3]
          "
        >

          © {new Date().getFullYear()} MukondoGTech AI.
          All rights reserved.

        </p>


      </motion.div>


    </main>

  );

}