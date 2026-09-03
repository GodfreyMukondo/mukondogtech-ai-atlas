import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { ArrowLeft, UserPlus } from "lucide-react";

import { createUser } from "../../api/userApi";


type Role = "USER" | "ADMIN";


interface CreateUserForm {
  fullName: string;
  email: string;
  password: string;
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



      await createUser(form);



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


      toast.error(
        "Failed to create user."
      );


    }
    finally{

      setLoading(false);

    }


  };








  return (

  <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-amber-50 px-6 py-12">


      <div className="mx-auto max-w-3xl">


        <button

          onClick={() => navigate(-1)}

          className="mb-6 flex items-center gap-2 rounded-xl bg-white px-4 py-2 shadow-sm"

        >

          <ArrowLeft size={18}/>

          Back

        </button>





        <div className="rounded-3xl bg-white p-8 shadow-xl">


          <div className="mb-8 flex items-center gap-4">


            <div className="rounded-2xl bg-blue-100 p-4 text-blue-700">

              <UserPlus size={30}/>

            </div>


            <div>

              <h1 className="text-3xl font-black text-slate-900">

                Create User

              </h1>


              <p className="text-slate-500">

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

              className="w-full rounded-xl border p-3"

            />





            <input

              name="email"

              type="email"

              value={form.email}

              onChange={handleChange}

              placeholder="Email address"

              required

              className="w-full rounded-xl border p-3"

            />






            <input

              name="password"

              type="password"

              value={form.password}

              onChange={handleChange}

              placeholder="Temporary password"

              required

              className="w-full rounded-xl border p-3"

            />






            <select

              name="role"

              value={form.role}

              onChange={handleChange}

              className="w-full rounded-xl border p-3"

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

              className="w-full rounded-xl bg-[#F4B81A] py-3 font-bold text-[#071330] disabled:opacity-50"

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