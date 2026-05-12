from __future__ import annotations

from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
TEAM_DIR = ROOT / '.codex' / 'team-md'
OUT = ROOT / '.codex' / 'sql' / 'ibank-team-seed.sql'

PASSWORD_HASH = '$2b$12$tzatvdD02DDLmrqIXLtCa.ucFmjdnydHO73TWemkBYQSHVAUnYNXW'

DEPARTMENTS = {
    1: {'name': '데이터컨설팅사업부', 'head': 2, 'description': '운영 데이터 품질, 정합성 분석, 리포트 검증을 담당하는 사업부'},
    2: {'name': '솔루션사업부', 'head': 3, 'description': '고객 운영, 정산, 권한, 현업 요구사항 대응을 담당하는 사업부'},
    3: {'name': '솔루션개발사업부', 'head': 4, 'description': '플랫폼 개발, 배포 안정화, AI 검색 및 인프라 협업을 담당하는 사업부'},
}

USERS = {
    1:  {'name': '윤태훈', 'department_id': None, 'position': '본부장', 'title': '경영총괄', 'role_code': 'DIRECTOR', 'join_date': '2018-03-05', 'email': 'director@ibank.local', 'phone': '02-6281-0001'},
    2:  {'name': '박서진', 'department_id': 1, 'position': '사업부장', 'title': '데이터컨설팅사업부장', 'role_code': 'DEPT_HEAD', 'join_date': '2019-01-14', 'email': 'u002@ibank.local', 'phone': '010-2000-0002'},
    3:  {'name': '최민석', 'department_id': 2, 'position': '사업부장', 'title': '솔루션사업부장', 'role_code': 'DEPT_HEAD', 'join_date': '2019-03-11', 'email': 'u003@ibank.local', 'phone': '010-2000-0003'},
    4:  {'name': '한지훈', 'department_id': 3, 'position': '사업부장', 'title': '솔루션개발사업부장', 'role_code': 'DEPT_HEAD', 'join_date': '2019-05-20', 'email': 'u004@ibank.local', 'phone': '010-2000-0004'},
    5:  {'name': '김도윤', 'department_id': 1, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-02-01', 'email': 'u005@ibank.local', 'phone': '010-2000-0005'},
    6:  {'name': '이서연', 'department_id': 1, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-03-08', 'email': 'u006@ibank.local', 'phone': '010-2000-0006'},
    7:  {'name': '정유진', 'department_id': 1, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-04-12', 'email': 'u007@ibank.local', 'phone': '010-2000-0007'},
    8:  {'name': '오현우', 'department_id': 1, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-05-17', 'email': 'u008@ibank.local', 'phone': '010-2000-0008'},
    9:  {'name': '임지민', 'department_id': 1, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-06-21', 'email': 'u009@ibank.local', 'phone': '010-2000-0009'},
    10: {'name': '윤가은', 'department_id': 1, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-07-05', 'email': 'u010@ibank.local', 'phone': '010-2000-0010'},
    11: {'name': '송태성', 'department_id': 1, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-08-09', 'email': 'u011@ibank.local', 'phone': '010-2000-0011'},
    12: {'name': '조하늘', 'department_id': 1, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-09-13', 'email': 'u012@ibank.local', 'phone': '010-2000-0012'},
    13: {'name': '백민지', 'department_id': 1, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-10-18', 'email': 'u013@ibank.local', 'phone': '010-2000-0013'},
    14: {'name': '장도현', 'department_id': 1, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-11-22', 'email': 'u014@ibank.local', 'phone': '010-2000-0014'},
    15: {'name': '신예린', 'department_id': 1, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2022-01-10', 'email': 'u015@ibank.local', 'phone': '010-2000-0015'},
    16: {'name': '강민호', 'department_id': 2, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-02-15', 'email': 'u016@ibank.local', 'phone': '010-2000-0016'},
    17: {'name': '노수진', 'department_id': 2, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-03-22', 'email': 'u017@ibank.local', 'phone': '010-2000-0017'},
    18: {'name': '문지후', 'department_id': 2, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-04-26', 'email': 'u018@ibank.local', 'phone': '010-2000-0018'},
    19: {'name': '배서윤', 'department_id': 2, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-05-31', 'email': 'u019@ibank.local', 'phone': '010-2000-0019'},
    20: {'name': '서진우', 'department_id': 2, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-07-12', 'email': 'u020@ibank.local', 'phone': '010-2000-0020'},
    21: {'name': '손나래', 'department_id': 2, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-08-16', 'email': 'u021@ibank.local', 'phone': '010-2000-0021'},
    22: {'name': '안태영', 'department_id': 2, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-09-27', 'email': 'u022@ibank.local', 'phone': '010-2000-0022'},
    23: {'name': '유정민', 'department_id': 2, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-10-25', 'email': 'u023@ibank.local', 'phone': '010-2000-1023'},
    24: {'name': '전하람', 'department_id': 2, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-11-29', 'email': 'u024@ibank.local', 'phone': '010-2000-0024'},
    25: {'name': '차은성', 'department_id': 2, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2022-01-17', 'email': 'u025@ibank.local', 'phone': '010-2000-0025'},
    26: {'name': '채도윤', 'department_id': 3, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-02-08', 'email': 'u026@ibank.local', 'phone': '010-2000-0026'},
    27: {'name': '천예준', 'department_id': 3, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-03-15', 'email': 'u027@ibank.local', 'phone': '010-2000-0027'},
    28: {'name': '최서아', 'department_id': 3, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-04-19', 'email': 'u028@ibank.local', 'phone': '010-2000-0028'},
    29: {'name': '하민재', 'department_id': 3, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-05-24', 'email': 'u029@ibank.local', 'phone': '010-2000-0029'},
    30: {'name': '허유나', 'department_id': 3, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-06-28', 'email': 'u030@ibank.local', 'phone': '010-2000-0030'},
    31: {'name': '황지후', 'department_id': 3, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-08-02', 'email': 'u031@ibank.local', 'phone': '010-2000-0031'},
    32: {'name': '김하린', 'department_id': 3, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-09-06', 'email': 'u032@ibank.local', 'phone': '010-2000-0032'},
    33: {'name': '이도겸', 'department_id': 3, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-10-11', 'email': 'u033@ibank.local', 'phone': '010-2000-0033'},
    34: {'name': '박연우', 'department_id': 3, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2021-11-15', 'email': 'u034@ibank.local', 'phone': '010-2000-0034'},
    35: {'name': '오세린', 'department_id': 3, 'position': '사원', 'title': None, 'role_code': 'MEMBER', 'join_date': '2022-01-24', 'email': 'u035@ibank.local', 'phone': '010-2000-0035'},
}

TEAM_ID_RE = re.compile(r'team_id: (\d+)')
TEAM_NAME_RE = re.compile(r'team_name: (.+)')
DEPT_RE = re.compile(r'owning_department_id: (\d+)')
STATUS_RE = re.compile(r'team_status_code: (\w+)')
START_RE = re.compile(r'start_date: (\d{4}-\d{2}-\d{2})')
END_RE = re.compile(r'expected_end_date: (\d{4}-\d{2}-\d{2})')
HEAD_RE = re.compile(r'주관 사업부장\(user_id\): (\d+)')
GOAL_RE = re.compile(r'- 왜 이 팀이 만들어졌는가\?\n\s*- (.+)')
TABLE_ROW_RE = re.compile(r'^\|\s*(\d+)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|$', re.M)


def sql_str(v):
    if v is None:
        return 'NULL'
    return "'" + str(v).replace("'", "''") + "'"


def parse_team_docs():
    teams = []
    for path in sorted(TEAM_DIR.glob('1*.md')):
        text = path.read_text(encoding='utf-8')
        team_id = int(TEAM_ID_RE.search(text).group(1))
        team_name = TEAM_NAME_RE.search(text).group(1).strip()
        dept_id = int(DEPT_RE.search(text).group(1))
        status = STATUS_RE.search(text).group(1)
        start = START_RE.search(text).group(1)
        end = END_RE.search(text).group(1)
        head_user_id = int(HEAD_RE.search(text).group(1))
        goal = GOAL_RE.search(text).group(1).strip()
        members = []
        for m in TABLE_ROW_RE.finditer(text):
            uid = int(m.group(1))
            members.append({
                'user_id': uid,
                'team_role': m.group(3).strip(),
                'allocation': m.group(4).strip(),
                'is_primary': m.group(5).strip().lower() == 'true',
                'is_leader': m.group(6).strip().lower() == 'true',
                'status_code': m.group(7).strip(),
            })
        teams.append({
            'team_id': team_id,
            'team_name': team_name,
            'department_id': dept_id,
            'status_code': status,
            'start_date': start,
            'expected_end_date': end,
            'description': goal,
            'head_user_id': head_user_id,
            'members': members,
        })
    return teams


def normalize_primary_memberships(teams):
    memberships_by_user = {}
    for team in teams:
        for member in team['members']:
            memberships_by_user.setdefault(member['user_id'], []).append((team, member))

    for memberships in memberships_by_user.values():
        active_team_memberships = [
            item for item in memberships
            if item[0]['status_code'] == 'ACTIVE' and item[1]['status_code'] == 'ACTIVE'
        ]
        active_primary_memberships = [
            item for item in active_team_memberships
            if item[1]['is_primary']
        ]
        if active_primary_memberships:
            chosen_team, chosen_member = sorted(
                active_primary_memberships,
                key=lambda item: (not item[1]['is_leader'], item[0]['team_id']),
            )[0]
        elif active_team_memberships:
            chosen_team, chosen_member = sorted(
                active_team_memberships,
                key=lambda item: (not item[1]['is_leader'], item[0]['team_id']),
            )[0]
        else:
            primary_memberships = [item for item in memberships if item[1]['is_primary']]
            if primary_memberships:
                chosen_team, chosen_member = sorted(
                    primary_memberships,
                    key=lambda item: (not item[1]['is_leader'], item[0]['team_id']),
                )[0]
            else:
                chosen_team, chosen_member = sorted(
                    memberships,
                    key=lambda item: (not item[1]['is_leader'], item[0]['team_id']),
                )[0]

        for team, member in memberships:
            member['is_primary'] = (team, member) == (chosen_team, chosen_member)


def build_sql():
    teams = parse_team_docs()
    if not teams:
        raise RuntimeError(f'No team markdown files found under {TEAM_DIR}')
    normalize_primary_memberships(teams)
    min_team_id = min(team['team_id'] for team in teams)
    max_team_id = max(team['team_id'] for team in teams)
    lines = []
    lines.append('-- iBank team seed SQL generated from .codex/team-md')
    lines.append('-- Scope: tb_department, tb_user, tb_team, tb_team_admin, tb_user_team')
    lines.append('-- Rules:')
    lines.append('--   1. user_id=1 is director/admin-only and is not used for team department ownership.')
    lines.append('--   2. tb_team.department_id follows the non-director tb_team_admin user department.')
    lines.append('--   3. Each user_id in tb_user_team has exactly one is_primary = true row.')
    lines.append('BEGIN;')
    lines.append('')

    lines.append('-- Departments: insert without head first to avoid the circular FK with tb_user.')
    lines.append('INSERT INTO tb_department (department_id, department_name, description, department_head_user_id, status_code) VALUES')
    dep_rows = []
    for dep_id in sorted(DEPARTMENTS):
        dep = DEPARTMENTS[dep_id]
        dep_rows.append(f"  ({dep_id}, {sql_str(dep['name'])}, {sql_str(dep['description'])}, NULL, 'ACTIVE')")
    lines.append(',\n'.join(dep_rows))
    lines.append('ON CONFLICT (department_id) DO UPDATE SET')
    lines.append('  department_name = EXCLUDED.department_name,')
    lines.append('  description = EXCLUDED.description,')
    lines.append('  status_code = EXCLUDED.status_code;')
    lines.append('')

    lines.append('-- Users')
    lines.append('INSERT INTO tb_user (user_id, department_id, user_name, email, password_hash, position_name, title_name, join_date, role_code, profile_image_url, phone, employment_status) VALUES')
    user_rows = []
    for uid in sorted(USERS):
        u = USERS[uid]
        user_rows.append(
            f"  ({uid}, {u['department_id'] if u['department_id'] is not None else 'NULL'}, {sql_str(u['name'])}, {sql_str(u['email'])}, {sql_str(PASSWORD_HASH)}, {sql_str(u['position'])}, {sql_str(u['title'])}, {sql_str(u['join_date'])}, {sql_str(u['role_code'])}, {sql_str(f'https://cdn.ibank.local/profiles/u{uid:03d}.png')}, {sql_str(u['phone'])}, 'ACTIVE')"
        )
    lines.append(',\n'.join(user_rows))
    lines.append('ON CONFLICT (user_id) DO UPDATE SET')
    lines.append('  department_id = EXCLUDED.department_id,')
    lines.append('  user_name = EXCLUDED.user_name,')
    lines.append('  email = EXCLUDED.email,')
    lines.append('  password_hash = EXCLUDED.password_hash,')
    lines.append('  position_name = EXCLUDED.position_name,')
    lines.append('  title_name = EXCLUDED.title_name,')
    lines.append('  join_date = EXCLUDED.join_date,')
    lines.append('  role_code = EXCLUDED.role_code,')
    lines.append('  profile_image_url = EXCLUDED.profile_image_url,')
    lines.append('  phone = EXCLUDED.phone,')
    lines.append('  employment_status = EXCLUDED.employment_status;')
    lines.append('')

    lines.append('-- Department heads after users exist.')
    for dep_id in sorted(DEPARTMENTS):
        lines.append(f"UPDATE tb_department SET department_head_user_id = {DEPARTMENTS[dep_id]['head']} WHERE department_id = {dep_id};")
    lines.append('')

    lines.append('-- Teams')
    lines.append('INSERT INTO tb_team (team_id, department_id, team_name, status_code, description, start_date, expected_end_date, deleted_at) VALUES')
    team_rows = []
    for team in teams:
        team_rows.append(
            f"  ({team['team_id']}, {team['department_id']}, {sql_str(team['team_name'])}, {sql_str(team['status_code'])}, {sql_str(team['description'])}, {sql_str(team['start_date'])}, {sql_str(team['expected_end_date'])}, NULL)"
        )
    lines.append(',\n'.join(team_rows))
    lines.append('ON CONFLICT (team_id) DO UPDATE SET')
    lines.append('  department_id = EXCLUDED.department_id,')
    lines.append('  team_name = EXCLUDED.team_name,')
    lines.append('  status_code = EXCLUDED.status_code,')
    lines.append('  description = EXCLUDED.description,')
    lines.append('  start_date = EXCLUDED.start_date,')
    lines.append('  expected_end_date = EXCLUDED.expected_end_date,')
    lines.append('  deleted_at = EXCLUDED.deleted_at;')
    lines.append('')

    lines.append('-- Team admins: owning department head + director(user_id=1)')
    admin_rows = []
    for team in teams:
        admin_rows.append(f"  ({team['head_user_id']}, {team['team_id']})")
        admin_rows.append(f"  (1, {team['team_id']})")
    lines.append('INSERT INTO tb_team_admin (user_id, team_id)')
    lines.append('SELECT v.user_id, v.team_id')
    lines.append('FROM (VALUES')
    lines.append(',\n'.join(admin_rows))
    lines.append(') AS v(user_id, team_id)')
    lines.append('ON CONFLICT (user_id, team_id) DO NOTHING;')
    lines.append('')

    lines.append("-- 팀 소유 부서는 director가 아닌 관리자 사용자의 부서를 따른다.")
    lines.append('UPDATE tb_team t')
    lines.append('SET department_id = u.department_id')
    lines.append('FROM tb_team_admin ta')
    lines.append('JOIN tb_user u ON u.user_id = ta.user_id')
    lines.append('WHERE ta.team_id = t.team_id')
    lines.append('  AND ta.user_id <> 1')
    lines.append(f'  AND t.team_id BETWEEN {min_team_id} AND {max_team_id}')
    lines.append('  AND t.department_id IS DISTINCT FROM u.department_id;')
    lines.append('')

    lines.append('DROP TABLE IF EXISTS tmp_ibank_user_team_seed;')
    lines.append('CREATE TEMP TABLE tmp_ibank_user_team_seed (')
    lines.append('  user_id BIGINT NOT NULL,')
    lines.append('  team_id BIGINT NOT NULL,')
    lines.append('  team_role VARCHAR(50) NOT NULL,')
    lines.append('  allocation VARCHAR(50),')
    lines.append('  is_primary BOOLEAN NOT NULL,')
    lines.append('  status_code VARCHAR(20) NOT NULL,')
    lines.append('  is_leader BOOLEAN NOT NULL')
    lines.append(') ON COMMIT DROP;')
    lines.append('')

    lines.append('-- Team members.')
    user_team_rows = []
    for team in teams:
        for m in team['members']:
            user_team_rows.append(
                f"  ({m['user_id']}, {team['team_id']}, {sql_str(m['team_role'])}, {sql_str(m['allocation'])}, {'true' if m['is_primary'] else 'false'}, {sql_str(m['status_code'])}, {'true' if m['is_leader'] else 'false'})"
            )
    lines.append('INSERT INTO tmp_ibank_user_team_seed (user_id, team_id, team_role, allocation, is_primary, status_code, is_leader) VALUES')
    lines.append(',\n'.join(user_team_rows) + ';')
    lines.append('')

    lines.append('-- Re-seeding must keep exactly one primary membership per user.')
    lines.append('UPDATE tb_user_team ut')
    lines.append('SET is_primary = false,')
    lines.append('    updated_at = CURRENT_TIMESTAMP')
    lines.append('FROM (')
    lines.append('  SELECT DISTINCT user_id')
    lines.append('  FROM tmp_ibank_user_team_seed')
    lines.append(') seeded_user')
    lines.append('WHERE ut.user_id = seeded_user.user_id')
    lines.append('  AND ut.is_primary = true;')
    lines.append('')

    lines.append('INSERT INTO tb_user_team (user_id, team_id, team_role, allocation, is_primary, status_code, is_leader)')
    lines.append('SELECT user_id, team_id, team_role, allocation, is_primary, status_code, is_leader')
    lines.append('FROM tmp_ibank_user_team_seed')
    lines.append('ON CONFLICT (user_id, team_id) DO UPDATE SET')
    lines.append('  team_role = EXCLUDED.team_role,')
    lines.append('  allocation = EXCLUDED.allocation,')
    lines.append('  is_primary = EXCLUDED.is_primary,')
    lines.append('  status_code = EXCLUDED.status_code,')
    lines.append('  is_leader = EXCLUDED.is_leader,')
    lines.append('  updated_at = CURRENT_TIMESTAMP;')
    lines.append('')

    lines.append('-- Sequence alignment for explicit IDs.')
    lines.append("SELECT setval(pg_get_serial_sequence('tb_department', 'department_id'), (SELECT COALESCE(MAX(department_id), 1) FROM tb_department), true);")
    lines.append("SELECT setval(pg_get_serial_sequence('tb_user', 'user_id'), (SELECT COALESCE(MAX(user_id), 1) FROM tb_user), true);")
    lines.append("SELECT setval(pg_get_serial_sequence('tb_team', 'team_id'), (SELECT COALESCE(MAX(team_id), 1) FROM tb_team), true);")
    lines.append("SELECT setval(pg_get_serial_sequence('tb_team_admin', 'team_admin_id'), (SELECT COALESCE(MAX(team_admin_id), 1) FROM tb_team_admin), true);")
    lines.append("SELECT setval(pg_get_serial_sequence('tb_user_team', 'user_team_id'), (SELECT COALESCE(MAX(user_team_id), 1) FROM tb_user_team), true);")
    lines.append('')
    lines.append('COMMIT;')
    lines.append('')
    return '\n'.join(lines)


def main():
    OUT.write_text(build_sql(), encoding='utf-8')
    print(f'Wrote {OUT}')

if __name__ == '__main__':
    main()
