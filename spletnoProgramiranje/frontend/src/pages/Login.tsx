import { useState } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { Bus, Mail, Lock, ArrowLeft, ArrowRight } from "lucide-react";
import { useUser } from "../context/UserContext";

export default function Login() {
  const { setIsAuthenticated, setIsAdmin, setUser: setUserContext } = useUser();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  const from =
    (location.state as any)?.from?.pathname || "/dashboard/interactive-map";

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setIsLoading(true);

    try {
      const response = await fetch(
        "http://20.208.138.248:8080/v1/authentication/login",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Accept: "application/json",
          },
          body: JSON.stringify({ email, password }),
        },
      );

      const data = await response.json();

      if (response.ok) {
        localStorage.setItem("authToken", data.token);

        const userData = {
          id: data.userId || data.id,
          username: data.username,
          email: email,
        };

        localStorage.setItem("user", JSON.stringify(userData));
        setIsAuthenticated(true);
        setUserContext(userData);

        if (data.isAdmin) {
          setIsAdmin(true);
        }

        navigate(from);
      } else {
        setError(data.message || "Invalid email or password");
      }
    } catch (error) {
      setError("Unable to connect to the server. Please try again.");
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex">
      {/* Left side - Form */}
      <div className="flex-1 flex flex-col justify-center py-12 px-4 sm:px-6 lg:px-20 xl:px-24">
        <div className="mx-auto w-full max-w-sm lg:w-96">
          {/* Back button */}
          <Link
            to="/"
            className="inline-flex items-center gap-2 text-gray-600 hover:text-marprom-600 mb-8 transition-colors"
          >
            <ArrowLeft className="h-4 w-4" />
            <span className="text-sm font-medium">Back to home</span>
          </Link>

          {/* Logo and heading */}
          <div>
            <Link to="/" className="flex items-center gap-2 mb-6">
              <div className="bg-marprom-600 p-2 rounded-xl">
                <Bus className="h-6 w-6 text-white" />
              </div>
              <span className="text-2xl font-bold text-gray-900">M-busi</span>
            </Link>
            <h2 className="text-3xl font-bold text-gray-900">Welcome back</h2>
            <p className="mt-2 text-sm text-gray-600">
              Don't have an account?{" "}
              <Link
                to="/register"
                className="font-medium text-marprom-600 hover:text-marprom-700"
              >
                Sign up for free
              </Link>
            </p>
          </div>

          {/* Form */}
          <div className="mt-8">
            {error && (
              <div className="mb-4 bg-red-50 border border-red-200 rounded-lg p-4">
                <p className="text-sm text-red-600">{error}</p>
              </div>
            )}

            <form onSubmit={handleLogin} className="space-y-6">
              <div>
                <label
                  htmlFor="email"
                  className="block text-sm font-medium text-gray-700 mb-2"
                >
                  Email address
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Mail className="h-5 w-5 text-gray-400" />
                  </div>
                  <input
                    id="email"
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="block w-full pl-10 pr-3 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-marprom-600 focus:border-transparent"
                    placeholder="you@example.com"
                  />
                </div>
              </div>

              <div>
                <label
                  htmlFor="password"
                  className="block text-sm font-medium text-gray-700 mb-2"
                >
                  Password
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Lock className="h-5 w-5 text-gray-400" />
                  </div>
                  <input
                    id="password"
                    type="password"
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="block w-full pl-10 pr-3 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-marprom-600 focus:border-transparent"
                    placeholder="••••••••"
                  />
                </div>
              </div>

              <div className="flex items-center justify-between">
                <div className="flex items-center">
                  <input
                    id="remember-me"
                    type="checkbox"
                    className="h-4 w-4 text-marprom-600 focus:ring-marprom-600 border-gray-300 rounded"
                  />
                  <label
                    htmlFor="remember-me"
                    className="ml-2 block text-sm text-gray-700"
                  >
                    Remember me
                  </label>
                </div>
                <a
                  href="#"
                  className="text-sm font-medium text-marprom-600 hover:text-marprom-700"
                >
                  Forgot password?
                </a>
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="w-full flex items-center justify-center gap-2 bg-marprom-600 text-white py-3 px-4 rounded-lg font-semibold hover:bg-marprom-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-marprom-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isLoading ? (
                  <>
                    <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                    <span>Signing in...</span>
                  </>
                ) : (
                  <>
                    <span>Sign in</span>
                    <ArrowRight className="h-5 w-5" />
                  </>
                )}
              </button>
            </form>
          </div>
        </div>
      </div>

      {/* Right side - Image/Illustration */}
      <div className="hidden lg:block relative w-0 flex-1">
        <div className="absolute inset-0 bg-gradient-to-br from-marprom-600 to-marprom-800">
          <div className="absolute inset-0 bg-black/20"></div>
          <img
            className="absolute inset-0 h-full w-full object-cover mix-blend-overlay opacity-40"
            src="https://images.pexels.com/photos/2338673/pexels-photo-2338673.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2"
            alt="Bus transit"
          />
          <div className="absolute inset-0 flex items-center justify-center p-12">
            <div className="max-w-md text-center text-white space-y-6">
              <h3 className="text-4xl font-bold">Track. Plan. Travel.</h3>
              <p className="text-xl text-white/90">
                Join thousands of commuters who use M-busi to make their daily
                travel in Maribor easier and more efficient.
              </p>
              <div className="flex items-center justify-center gap-8 pt-6">
                <div>
                  <div className="text-3xl font-bold">42+</div>
                  <div className="text-sm text-white/80">Bus Routes</div>
                </div>
                <div className="h-12 w-px bg-white/30"></div>
                <div>
                  <div className="text-3xl font-bold">25K+</div>
                  <div className="text-sm text-white/80">Daily Users</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
