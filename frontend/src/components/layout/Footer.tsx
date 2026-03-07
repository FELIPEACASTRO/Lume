export default function Footer() {
  return (
    <footer className="bg-white border-t border-dark-100 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
        <p className="text-center text-dark-400 text-sm">
          &copy; {new Date().getFullYear()} Lume. Todos os direitos reservados.
        </p>
      </div>
    </footer>
  );
}
