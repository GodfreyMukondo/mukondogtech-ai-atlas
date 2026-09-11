import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import axios from "axios";
import { ArrowLeft, UserPlus } from "lucide-react";

import { createUser } from "../../api/userApi";


type Role = "USER" | "ADMIN";


interface CreateUserForm {
  fullName: string;
  email: string;
  password: string;
  phone: string;
  country: string;
  role: Role;
}



export default function CreateUserPage() {


  const navigate = useNavigate();


  const [loading, setLoading] = useState(false);


  const [form, setForm] =
    useState<CreateUserForm>({
      fullName: "",
      email: "",
      password: "",
      phone: "",
      country: "",
      role: "USER",
    });





  const handleChange = (
    event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {


    const {name, value} = event.target;


    setForm(prev => ({
      ...prev,
      [name]: value,
    }));

  };








  const handleSubmit = async (
    event: React.FormEvent
  ) => {


    event.preventDefault();


    if(loading) return;



    try {


      setLoading(true);



      await createUser({
        ...form,
        phone: form.phone.trim() || undefined,
        country: form.country.trim() || undefined,
      });



      toast.success(
        "User created successfully."
      );



      navigate("/admin/users");



    }
    catch(error){


      console.error(
        "Create user failed:",
        error
      );


      const message =
        axios.isAxiosError(error)
          ? error.response?.data?.message ?? "Failed to create user."
          : "Failed to create user.";


      toast.error(message);


    }
    finally{

      setLoading(false);

    }


  };








  return (

  <div className="min-h-screen px-6 py-12">


      <div className="mx-auto max-w-3xl">


        <button

          onClick={() => navigate(-1)}

          className="mb-6 flex items-center gap-2 rounded-xl border border-white/15 bg-white/5 text-slate-200 px-4 py-2 shadow-sm hover:bg-white/10 transition"

        >

          <ArrowLeft size={18}/>

          Back

        </button>





        <div className="rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl p-8 shadow-xl">


          <div className="mb-8 flex items-center gap-4">


            <div className="rounded-2xl bg-blue-500/10 p-4 text-blue-300">

              <UserPlus size={30}/>

            </div>


            <div>

              <h1 className="text-3xl font-black text-white">

                Create User

              </h1>


              <p className="text-slate-400">

                Add a new platform user.

              </p>


            </div>


          </div>







          <form
            onSubmit={handleSubmit}
            className="space-y-5"
          >



            <input

              name="fullName"

              value={form.fullName}

              onChange={handleChange}

              placeholder="Full name"

              required

              className="w-full rounded-xl border border-white/15 bg-white/5 text-white placeholder:text-slate-500 p-3"

            />





            <input

              name="email"

              type="email"

              value={form.email}

              onChange={handleChange}

              placeholder="Email address"

              required

              className="w-full rounded-xl border border-white/15 bg-white/5 text-white placeholder:text-slate-500 p-3"

            />






            <input

              name="password"

              type="password"

              value={form.password}

              onChange={handleChange}

              placeholder="Temporary password"

              required

              className="w-full rounded-xl border border-white/15 bg-white/5 text-white placeholder:text-slate-500 p-3"

            />




            <div className="grid grid-cols-2 gap-4">

              <input

                name="phone"

                type="tel"

                value={form.phone}

                onChange={handleChange}

                placeholder="Phone (optional)"

                className="w-full rounded-xl border border-white/15 bg-white/5 text-white placeholder:text-slate-500 p-3"

              />

              <input

                name="country"

                value={form.country}

                onChange={handleChange}

                placeholder="Country (optional)"

                className="w-full rounded-xl border border-white/15 bg-white/5 text-white placeholder:text-slate-500 p-3"

              />

            </div>




            <select

              name="role"

              value={form.role}

              onChange={handleChange}

              className="w-full rounded-xl border border-white/15 bg-white/5 text-white placeholder:text-slate-500 p-3"

            >

              <option value="USER">
                User
              </option>


              <option value="ADMIN">
                Administrator
              </option>


            </select>






            <button

              disabled={loading}

              className="w-full rounded-xl bg-[#C6A15B] py-3 font-bold text-[#071426] disabled:opacity-50"

            >

              {loading
                ? "Creating..."
                : "Create User"
              }


            </button>




          </form>


        </div>


      </div>


    </div>

  );


}