import assert from "node:assert/strict";
import test from "node:test";
import {
  addTeamMemberSelection,
  filterTeamUserCandidates,
  getCandidateRankName,
} from "../src/app/(protected)/team/_components/teamForm.utils.ts";

const candidates = [
  {
    userId: 1,
    userName: "이재범",
    email: "director@ax-wms.com",
    phone: null,
    departmentId: 10,
    departmentName: "데이터컨설팅사업부",
    profileImageUrl: null,
    teamId: null,
    teamName: null,
    positionName: "본부장",
    titleName: "전무",
    employmentStatus: "ACTIVE",
  },
  {
    userId: 2,
    userName: "한서윤",
    email: "head-data@ax-wms.com",
    phone: null,
    departmentId: 10,
    departmentName: "데이터컨설팅사업부",
    profileImageUrl: null,
    teamId: null,
    teamName: null,
    positionName: "사업부장",
    titleName: "부장",
    employmentStatus: "ACTIVE",
  },
  {
    userId: 3,
    userName: "오민석",
    email: "head-sales@ax-wms.com",
    phone: null,
    departmentId: 20,
    departmentName: "솔루션사업부",
    profileImageUrl: null,
    teamId: null,
    teamName: null,
    positionName: "사업부장",
    titleName: "부장",
    employmentStatus: "ACTIVE",
  },
];

test("filters candidates by query, department, and rank", () => {
  const result = filterTeamUserCandidates(candidates, {
    query: "한서윤",
    departmentName: "데이터컨설팅사업부",
    rankName: "부장",
  });

  assert.deepEqual(
    result.map((candidate) => candidate.userId),
    [2],
  );
});

test("uses title before position for candidate rank display", () => {
  assert.equal(getCandidateRankName(candidates[0]), "전무");
});

test("adds first selected member as leader and skips duplicates", () => {
  const first = addTeamMemberSelection([], 1);
  const duplicate = addTeamMemberSelection(first, 1);
  const second = addTeamMemberSelection(first, 2);

  assert.deepEqual(first, [{ userId: 1, teamRole: "", isLeader: true }]);
  assert.equal(duplicate, first);
  assert.deepEqual(second, [
    { userId: 1, teamRole: "", isLeader: true },
    { userId: 2, teamRole: "", isLeader: false },
  ]);
});
