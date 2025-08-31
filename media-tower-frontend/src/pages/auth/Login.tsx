import { useState, FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { signInWithEmailAndPassword, signInWithPopup, GoogleAuthProvider } from 'firebase/auth';
import { auth } from '../../lib/firebase';
import { FirebaseError } from 'firebase/app';
import { Loader2 } from 'lucide-react';

const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [emailLoading, setEmailLoading] = useState(false);
  const [googleLoading, setGoogleLoading] = useState(false);
  const navigate = useNavigate();
  const googleProvider = new GoogleAuthProvider();

  const handleSuccess = () => {
    navigate('/auth-redirect');
  };

  const handleLogin = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setEmailLoading(true);
    try {
      await signInWithEmailAndPassword(auth, email, password);
      handleSuccess();
    } catch (err: unknown) { // On type 'err' comme unknown pour la sécurité
      if (err instanceof FirebaseError && (err.code === 'auth/user-not-found' || err.code === 'auth/wrong-password' || err.code === 'auth/invalid-credential')) {
        setError('Failed to log in. Check your email and password.');
      } else {
        setError('An unexpected error occurred.');
      }
      // On log l'erreur pour le débogage, ce qui utilise la variable 'err'
      console.error("Login failed:", err);
    } finally {
      setEmailLoading(false);
    }
  };

  const handleGoogleLogin = async () => {
    setError('');
    setGoogleLoading(true);
    try {
      await signInWithPopup(auth, googleProvider);
      handleSuccess();
    } catch (err: unknown) { // On type 'err'
      setError('Failed to log in with Google.');
      // On log l'erreur
      console.error("Google login failed:", err);
    } finally {
      setGoogleLoading(false);
    }
  };

  return (
      <div className="flex items-center justify-center min-h-screen bg-gray-900 text-white">
        <div className="w-full max-w-md p-8 space-y-6 bg-card rounded-lg shadow-md">
          <h1 className="text-2xl font-bold text-center text-foreground">Login to Media Tower</h1>
          {error && <p className="text-red-500 text-center">{error}</p>}
          <form onSubmit={handleLogin} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-400">Email</label>
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} className="w-full px-3 py-2 mt-1 text-white bg-gray-700 border border-gray-600 rounded-md" required />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-400">Password</label>
              <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} className="w-full px-3 py-2 mt-1 text-white bg-gray-700 border border-gray-600 rounded-md" required />
            </div>
            <button type="submit" disabled={emailLoading || googleLoading} className="w-full py-2 font-bold text-white bg-primary rounded-md hover:bg-blue-600 disabled:bg-gray-500">
              {emailLoading ? <Loader2 className="animate-spin mx-auto"/> : 'Login'}
            </button>
          </form>
          <div className="relative my-4">
            <div className="absolute inset-0 flex items-center"><div className="w-full border-t border-gray-600" /></div>
            <div className="relative flex justify-center text-sm"><span className="px-2 bg-card text-gray-400">Or continue with</span></div>
          </div>
          <button onClick={handleGoogleLogin} disabled={emailLoading || googleLoading} className="w-full py-2 font-bold text-white bg-red-600 rounded-md hover:bg-red-700 disabled:bg-gray-500">
            {googleLoading ? <Loader2 className="animate-spin mx-auto"/> : 'Login with Google'}
          </button>
          <p className="text-sm text-center text-gray-400">
            Don't have an account? <a href="/register" className="font-medium text-primary hover:underline">Register</a>
          </p>
        </div>
      </div>
  );
};

export default Login;