import type { SelectOption } from "@/components/ui/select";

export const positionOptions: SelectOption[] = [
  { value: "사원", label: "사원" },
  { value: "대리", label: "대리" },
  { value: "과장", label: "과장" },
  { value: "차장", label: "차장" },
  { value: "부장", label: "부장" },
  { value: "상무", label: "상무" },
  { value: "이사", label: "이사" },
];

export const titleOptions: SelectOption[] = [
  { value: "본부장", label: "본부장" },
  { value: "사업부장", label: "사업부장" },
  { value: "팀장", label: "팀장" },
  { value: "팀원", label: "팀원" },
];
