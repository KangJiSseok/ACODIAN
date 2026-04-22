"use client";

import type { ReactNode } from "react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

type ConfirmDialogTone = "default" | "destructive";

interface ConfirmDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: string;
  confirmText?: string;
  cancelText?: string;
  onConfirm: () => void | Promise<void>;
  tone?: ConfirmDialogTone;
  confirmDisabled?: boolean;
  children?: ReactNode;
}

export default function ConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmText = "확인",
  cancelText = "취소",
  onConfirm,
  tone = "default",
  confirmDisabled = false,
  children,
}: ConfirmDialogProps) {
  const [isPending, setIsPending] = useState(false);

  // 확인 처리 중에는 오버레이 클릭이나 취소로 닫히지 않게 막습니다.
  const handleOpenChange = (nextOpen: boolean) => {
    if (isPending) {
      return;
    }

    onOpenChange(nextOpen);
  };

  // 비동기 confirm 액션이 여러 번 실행되지 않도록 pending 상태를 잠급니다.
  const handleConfirm = async () => {
    if (isPending || confirmDisabled) {
      return;
    }

    setIsPending(true);

    try {
      await onConfirm();
      onOpenChange(false);
    } finally {
      setIsPending(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent
        className="workspace-panel w-full max-w-lg rounded-3xl border border-border/80 p-6"
        aria-busy={isPending}
      >
        <DialogHeader className="gap-2">
          <DialogTitle className="text-xl tracking-tight">{title}</DialogTitle>
          <DialogDescription className="leading-6">{description}</DialogDescription>
        </DialogHeader>

        {/* 삭제 대상 요약처럼 다이얼로그 본문에 추가 정보가 필요할 때 사용합니다. */}
        {children ? <div className="mt-4">{children}</div> : null}

        <DialogFooter className="mt-6 flex-col-reverse gap-2 sm:flex-row">
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={isPending}
            className="w-full sm:w-auto"
          >
            {cancelText}
          </Button>
          <Button
            type="button"
            variant={tone === "destructive" ? "destructive" : "default"}
            onClick={handleConfirm}
            disabled={isPending || confirmDisabled}
            className="w-full sm:w-auto"
          >
            {isPending ? "처리 중..." : confirmText}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
