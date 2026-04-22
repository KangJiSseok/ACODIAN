import { useEffect, useState } from "react"
import { getVisibleWorklogs } from "../_utils/accessControl"
import { useAuth } from "./useAuth"
import { subscribeMockDb, teams } from "../_mock/worklog.mock"
import { worklogService } from "../_service/worklog.service"
import type { Worklog } from "../_types/worklog.types"

export function useWorklog() {
  const { user } = useAuth()
  const [worklogs, setWorklogs] = useState<Worklog[]>([])

  useEffect(() => {
    const sync = async () => {
      const items = await worklogService.list()
      setWorklogs(getVisibleWorklogs(user, items, teams))
    }

    void sync()
    return subscribeMockDb(() => {
      void sync()
    })
  }, [user])

  return {
    worklogs,
    refresh: async () =>
      setWorklogs(getVisibleWorklogs(user, await worklogService.list(), teams)),
  }
}
