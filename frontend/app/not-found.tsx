import Link from "next/link";

export default function NotFound() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-12">
      <section className="w-full max-w-[480px] rounded-xl bg-surface p-8 text-center shadow-md sm:p-10">
        <div className="mx-auto mb-6 grid h-12 w-12 place-items-center rounded-lg bg-gradient-to-br from-primary to-accent text-lg font-extrabold text-primary-foreground">
          T
        </div>
        <p className="text-sm font-semibold text-primary">404</p>
        <h1 className="mt-2 text-2xl font-bold text-foreground">페이지를 찾을 수 없습니다</h1>
        <p className="mt-3 text-sm leading-relaxed text-muted">
          주소가 잘못되었거나 페이지가 이동되었을 수 있습니다.
          <br />로그인 화면에서 다시 시작해주세요.
        </p>
        <Link
          href="/login"
          className="mt-8 block w-full rounded-lg btn-gradient py-3 text-sm font-semibold text-primary-foreground"
        >
          로그인으로 돌아가기
        </Link>
      </section>
    </main>
  );
}
