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

test("notification center fetches all recent notifications and unread count separately", () => {
  assert.match(notificationListHookFile, /const notificationListQuery = useNotificationList/);
  assert.match(notificationListHookFile, /pageSize:\s*100/);
  assert.match(notificationListHookFile, /const unreadCountQuery = useNotificationList/);
  assert.match(notificationListHookFile, /isRead:\s*false/);
  assert.match(notificationListHookFile, /pageSize:\s*1/);
  assert.match(notificationListHookFile, /notificationCenterNotifications: notificationListQuery\.notifications/);
});
