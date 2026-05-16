import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const notificationListHookFile = readFileSync(
  new URL(
    "../src/app/(protected)/notification/_hooks/useNotificationList.ts",
    import.meta.url,
  ),
  "utf8",
);

test("notification queries are scoped by current user and always refetch on mount", () => {
  assert.match(notificationListHookFile, /selectAuthUser/);
  assert.match(notificationListHookFile, /const userId = user\?\.userId/);
  assert.match(notificationListHookFile, /notificationKeys\.list\(userId,\s*params\)/);
  assert.match(notificationListHookFile, /staleTime:\s*0/);
  assert.match(notificationListHookFile, /refetchOnMount:\s*"always"/);
});

test("notification center fetches only unread notifications capped at ten", () => {
  assert.match(notificationListHookFile, /const notificationListQuery = useNotificationList/);
  assert.match(notificationListHookFile, /isRead:\s*false/);
  assert.match(notificationListHookFile, /pageSize:\s*10/);
  assert.match(notificationListHookFile, /filter\(\s*\(notification\) => !notification\.isRead,\s*\)/);
  assert.match(notificationListHookFile, /unreadCount: notificationListQuery\.notificationPage\?\.totalCount \?\? 0/);
  assert.match(notificationListHookFile, /notificationCenterNotifications: unreadNotifications/);
  assert.doesNotMatch(notificationListHookFile, /pageSize:\s*100/);
});
