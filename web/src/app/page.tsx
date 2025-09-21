import { ThemeToggle } from '@/components/ThemeToggle'

export default function Home() {
  return (
    <div className="min-h-screen bg-[var(--bg)] text-[var(--fg)]">
      <ThemeToggle />
      <div className="container mx-auto px-4 py-16">
        <div className="text-center max-w-4xl mx-auto">
          <h1 className="text-6xl font-bold mb-6 font-[family-name:var(--font-press-start-2p)]">
            LambdaCraft
          </h1>
          <p className="text-2xl text-[var(--muted)] mb-8 font-[family-name:var(--font-press-start-2p)]">
            () =&gt; study(math) =&gt; practice(code)
          </p>
        </div>
      </div>
    </div>
  )
}
