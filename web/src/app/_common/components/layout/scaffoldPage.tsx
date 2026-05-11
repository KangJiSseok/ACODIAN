import type { ReactNode } from "react";
import PageHeader from "./pageHeader";

interface ScaffoldPageProps {
  title: string;
  description: string;
  actions?: ReactNode;
  contentVariant?: "panel" | "plain";
  children?: ReactNode;
}

export default function ScaffoldPage({
  title,
  description,
  actions,
  contentVariant = "panel",
  children,
}: ScaffoldPageProps) {
  const content = children ?? (
    // 아직 구현 전인 화면도 같은 placeholder 톤을 유지하도록 공통 문구를 씁니다.
    <p className="text-sm leading-6 text-muted-foreground">
      이 화면은 현재 기본 라우팅과 레이아웃만 연결된 상태입니다. 이후 shadcn/ui와 도메인
      컴포넌트를 붙여 실제 UI를 확장하면 됩니다.
    </p>
  );

  return (
    <section className="space-y-6">
      <PageHeader title={title} description={description} actions={actions} />
      {contentVariant === "plain" ? (
        content
      ) : (
        <div className="workspace-panel rounded-3xl p-6 md:p-8">
          {content}
        </div>
      )}
    </section>
  );
}
