export function Footer() {
  return (
    <footer className="border-t border-gray-200 py-16 px-6 bg-white">
      <div className="max-w-6xl mx-auto">
        {/* Logo */}
        <div className="mb-8">
          <a href="#" className="inline-flex items-center gap-2">
            <img src="/src/assets/images/common/logo-dark-2.svg" alt="메아리 로고" className="h-8" />
          </a>
        </div>

        <div className="mt-12 pt-8 border-t border-gray-200 flex flex-col sm:flex-row items-center justify-between gap-4">
          <p className="text-navy-faint text-sm">
            &copy; 2026 메아리(Meari). All rights reserved.
          </p>
        </div>
      </div>
    </footer>
  )
}
