import LoginClient from "./loginClient";

type LoginPageProps = {
  searchParams?: Promise<{
    redirect?: string | string[];
  }>;
};

export default async function LoginPage({ searchParams }: LoginPageProps) {
  const resolvedSearchParams = await searchParams;
  const redirectParam = resolvedSearchParams?.redirect;
  const redirectPath = Array.isArray(redirectParam)
    ? redirectParam[0]
    : redirectParam ?? null;

  return <LoginClient redirectPath={redirectPath} />;
}
