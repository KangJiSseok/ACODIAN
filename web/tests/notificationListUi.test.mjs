import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const notificationListFile = readFileSync(
  new URL(
    "../src/app/(protected)/notification/_components/notificationList.tsx",
    import.meta.url,
  ),
  "utf8",
);

test("notification list keeps title meta inline without extra help chrome", () => {
  assert.doesNotMatch(notificationListFile, /CircleHelp/);
  assert.doesNotMatch(notificationListFile, /NotificationMetaBadge/);
  assert.doesNotMatch(notificationListFile, /aria-label="알림 상태 도움말"/);
  assert.doesNotMatch(notificationListFile, /getNotificationTypeLabel/);
  assert.doesNotMatch(notificationListFile, /Clock3/);
  assert.match(notificationListFile, /getNotificationReadLabel/);
});

test("notification list does not render read timestamp", () => {
  assert.doesNotMatch(
    notificationListFile,
    /formatDateTime\(notification\.readAt\)/,
  );
});

test("notification list hides backend date suffix from content and meta row", () => {
  assert.match(notificationListFile, /getNotificationDisplayContent/);
  assert.match(notificationListFile, /확인 기준일/);
  assert.doesNotMatch(notificationListFile, /getNotificationDueDateLabel/);
  assert.doesNotMatch(notificationListFile, /dueDateLabel/);
});

test("notification list renders received date at the card bottom", () => {
  assert.match(notificationListFile, /<time/);
  assert.match(notificationListFile, /dateTime=\{notification\.createdAt\}/);
  assert.match(
    notificationListFile,
    /formatNotificationCreatedAt\(notification\.createdAt\)/,
  );
});

test("notification item actions follow worklog detail and edit button variants", () => {
  assert.match(
    notificationListFile,
    /variant="default"[\s\S]*onClick=\{\(\) => onMarkRead\(notification\.id\)\}/,
  );
  assert.match(notificationListFile, /variant="secondary"[\s\S]*관련 화면 이동/);
  assert.match(notificationListFile, /h-11 px-5 text-sm font-semibold/);
  assert.doesNotMatch(notificationListFile, /shadow-\[/);
  assert.doesNotMatch(notificationListFile, /className="h-10 min-w-32 rounded-2xl/);
  assert.match(notificationListFile, /관련 화면 이동/);
});

test("notification card keeps bottom action with worklog-list spacing", () => {
  assert.doesNotMatch(notificationListFile, /lg:flex-row lg:items-start/);
  assert.match(notificationListFile, /flex flex-col gap-4 p-5/);
  assert.match(notificationListFile, /sm:flex-row sm:items-end sm:justify-between/);
  assert.match(notificationListFile, /p-5/);
  assert.match(notificationListFile, /line-clamp-2/);
  assert.match(notificationListFile, /leading-7/);
});
