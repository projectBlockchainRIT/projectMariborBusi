import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  Bus,
  Mail,
  Lock,
  User,
  ArrowLeft,
  ArrowRight,
  CheckCircle,
} from "lucide-react";
import { getApiUrl } from "../config/api";

export default function Register() {
  const [email, setEmail] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (password !== confirmPassword) {
      setError("Passwords do not match");
      return;
    }

    if (password.length < 6) {
      setError("Password must be at least 6 characters");
      return;
    }

    setIsLoading(true);

    try {
      const response = await fetch(
        getApiUrl("authentication/register"),
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ email, username, password }),
        },
      );

      const data = await response.json();

      if (response.ok) {
        if (data.token) {
          localStorage.setItem("authToken", data.token);
          localStorage.setItem("user", JSON.stringify({ email, username }));
          navigate("/dashboard");
        } else {
          navigate("/login", {
            state: { message: "Registration successful! Please log in." },
          });
        }
      } else {
        setError(data.message || "Registration failed. Please try again.");
      }
    } catch (error) {
      console.error("Error during registration:", error);
      setError("Unable to connect to the server. Please try again.");
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
            <h2 className="text-3xl font-bold text-gray-900">Create account</h2>
            <p className="mt-2 text-sm text-gray-600">
              Already have an account?{" "}
              <Link
                to="/login"
                className="font-medium text-marprom-600 hover:text-marprom-700"
              >
                Sign in
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

            <form onSubmit={handleSubmit} className="space-y-5">
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
                  htmlFor="username"
                  className="block text-sm font-medium text-gray-700 mb-2"
                >
                  Username
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <User className="h-5 w-5 text-gray-400" />
                  </div>
                  <input
                    id="username"
                    type="text"
                    required
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    className="block w-full pl-10 pr-3 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-marprom-600 focus:border-transparent"
                    placeholder="johndoe"
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

              <div>
                <label
                  htmlFor="confirm-password"
                  className="block text-sm font-medium text-gray-700 mb-2"
                >
                  Confirm Password
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Lock className="h-5 w-5 text-gray-400" />
                  </div>
                  <input
                    id="confirm-password"
                    type="password"
                    required
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    className="block w-full pl-10 pr-3 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-marprom-600 focus:border-transparent"
                    placeholder="••••••••"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="w-full flex items-center justify-center gap-2 bg-marprom-600 text-white py-3 px-4 rounded-lg font-semibold hover:bg-marprom-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-marprom-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isLoading ? (
                  <>
                    <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                    <span>Creating account...</span>
                  </>
                ) : (
                  <>
                    <span>Create account</span>
                    <ArrowRight className="h-5 w-5" />
                  </>
                )}
              </button>

              <p className="text-xs text-center text-gray-500 mt-4">
                By creating an account, you agree to our{" "}
                <a href="#" className="text-marprom-600 hover:text-marprom-700">
                  Terms
                </a>{" "}
                and{" "}
                <a href="#" className="text-marprom-600 hover:text-marprom-700">
                  Privacy Policy
                </a>
              </p>
            </form>
          </div>
        </div>
      </div>

      {/* Right side - Benefits */}
      <div className="hidden lg:block relative w-0 flex-1">
        <div className="absolute inset-0 bg-gradient-to-br from-marprom-600 to-marprom-800">
          <div className="absolute inset-0 bg-black/20"></div>
          <img
            className="absolute inset-0 h-full w-full object-cover mix-blend-overlay opacity-40"
            src="https://images.pexels.com/photos/1084540/pexels-photo-1084540.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2"
            alt="Public transit"
          />
          <div className="absolute inset-0 flex items-center justify-center p-12">
            <div className="max-w-md space-y-8">
              <div className="text-center text-white space-y-4">
                <h3 className="text-4xl font-bold">Start your journey</h3>
                <p className="text-xl text-white/90">
                  Get instant access to real-time bus tracking and smart route
                  planning
                </p>
              </div>

              <div className="space-y-4 bg-white/10 backdrop-blur-sm rounded-2xl p-6">
                <div className="flex items-start gap-3">
                  <CheckCircle className="h-6 w-6 text-white flex-shrink-0 mt-0.5" />
                  <div className="text-white">
                    <div className="font-semibold">Real-time tracking</div>
                    <div className="text-sm text-white/80">
                      See bus locations live on the map
                    </div>
                  </div>
                </div>
                <div className="flex items-start gap-3">
                  <CheckCircle className="h-6 w-6 text-white flex-shrink-0 mt-0.5" />
                  <div className="text-white">
                    <div className="font-semibold">Smart notifications</div>
                    <div className="text-sm text-white/80">
                      Get alerts for delays and changes
                    </div>
                  </div>
                </div>
                <div className="flex items-start gap-3">
                  <CheckCircle className="h-6 w-6 text-white flex-shrink-0 mt-0.5" />
                  <div className="text-white">
                    <div className="font-semibold">Route planning</div>
                    <div className="text-sm text-white/80">
                      Find the best way to your destination
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
