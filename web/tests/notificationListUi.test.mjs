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
  assert.match(notificationListFile, /getNotificationReadLabel/);
});

test("notification list does not render read timestamp", () => {
  assert.doesNotMatch(
    notificationListFile,
    /formatDateTime\(notification\.readAt\)/,
  );
});

test("notification list moves due date to the meta row instead of created timestamp", () => {
  assert.match(notificationListFile, /getNotificationDueDateLabel/);
  assert.match(notificationListFile, /getNotificationContentWithoutDueDate/);
  assert.match(notificationListFile, /마감일 :/);
  assert.doesNotMatch(
    notificationListFile,
    /formatDateTime\(notification\.createdAt\)/,
  );
});

test("notification deep link action uses primary worklog-style button", () => {
  assert.match(notificationListFile, /variant="default"/);
  assert.match(notificationListFile, /min-w-32/);
  assert.match(notificationListFile, /!text-primary-foreground/);
  assert.match(notificationListFile, /관련 화면 이동/);
});

test("notification card keeps bottom action with worklog-list spacing", () => {
  assert.doesNotMatch(notificationListFile, /lg:flex-row lg:items-start/);
  assert.match(notificationListFile, /flex flex-col gap-4 p-5/);
  assert.match(notificationListFile, /sm:flex-row sm:items-center sm:justify-between/);
  assert.match(notificationListFile, /p-5/);
  assert.match(notificationListFile, /line-clamp-2/);
  assert.match(notificationListFile, /leading-7/);
});
