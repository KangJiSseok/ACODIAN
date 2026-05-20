import type { ReactNode } from "react";

interface PageHeaderProps {
  title: ReactNode;
  description?: string;
  actions?: ReactNode;
}

export default function PageHeader({ title, description, actions }: PageHeaderProps) {
  if (!description && !actions) {
    return <h1 className="sr-only">{title}</h1>;
  }

  return (
    // 페이지 공통 타이틀, 설명, 우측 액션 영역의 배치를 한 곳에서 맞춥니다.
    <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
      <div className="min-w-0">
        <h1 className="sr-only">{title}</h1>
        {description ? (
          <p className="max-w-3xl text-[15px] leading-6 text-muted-foreground">{description}</p>
        ) : null}
      </div>
      {actions ? <div className="flex flex-wrap items-center gap-2 md:justify-end">{actions}</div> : null}
    </div>
  );
}
