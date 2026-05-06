"use client"

import type { tags, worklogs } from "../_mock/worklog.mock"
import type { WorklogFormValues } from "../_types/worklog.types"
import { WorklogDependencyModal } from "./worklogDependencyModal"
import { WorklogSettingsModal } from "./worklogSettingsModal"
import { WorklogTagModal } from "./worklogTagModal"

export type WorklogFormModalKey = "dependencies" | "settings" | "tags"

type SelectOption = {
  label: string
  value: string
}

type WorklogItem = (typeof worklogs)[number]
type TagItem = (typeof tags)[number]

interface WorklogFormModalsProps {
  activeModal: WorklogFormModalKey | null
  onActiveModalChange: (modal: WorklogFormModalKey | null) => void
  values: WorklogFormValues
  onValuesChange: (values: WorklogFormValues) => void
  controlClassName: string
  searchControlClassName: string
  teamOptions: SelectOption[]
  authorOptions: SelectOption[]
  statusOptions: SelectOption[]
  dependencyKeywordInput: string
  onDependencyKeywordInputChange: (value: string) => void
  dependencySearchOpen: boolean
  onDependencySearchOpenChange: (open: boolean) => void
  filteredDependencyCandidates: WorklogItem[]
  selectedDependencies: WorklogItem[]
  onAddDependency: (dependencyId: number) => void
  onRemoveDependency: (dependencyId: number) => void
  incompleteDependencies: WorklogItem[]
  circularDependencyDetected: boolean
  tagKeywordInput: string
  onTagKeywordInputChange: (value: string) => void
  tagSearchOpen: boolean
  onTagSearchOpenChange: (open: boolean) => void
  filteredTagCandidates: TagItem[]
  selectedTags: TagItem[]
  onAddTag: (tagId: number) => void
  onRemoveTag: (tagId: number) => void
}

export function WorklogFormModals({
  activeModal,
  onActiveModalChange,
  values,
  onValuesChange,
  controlClassName,
  searchControlClassName,
  teamOptions,
  authorOptions,
  statusOptions,
  dependencyKeywordInput,
  onDependencyKeywordInputChange,
  dependencySearchOpen,
  onDependencySearchOpenChange,
  filteredDependencyCandidates,
  selectedDependencies,
  onAddDependency,
  onRemoveDependency,
  incompleteDependencies,
  circularDependencyDetected,
  tagKeywordInput,
  onTagKeywordInputChange,
  tagSearchOpen,
  onTagSearchOpenChange,
  filteredTagCandidates,
  selectedTags,
  onAddTag,
  onRemoveTag,
}: WorklogFormModalsProps) {
  return (
    <>
      <WorklogDependencyModal
        open={activeModal === "dependencies"}
        onOpenChange={(open) =>
          onActiveModalChange(open ? "dependencies" : null)
        }
        status={values.status}
        searchControlClassName={searchControlClassName}
        dependencyKeywordInput={dependencyKeywordInput}
        onDependencyKeywordInputChange={onDependencyKeywordInputChange}
        dependencySearchOpen={dependencySearchOpen}
        onDependencySearchOpenChange={onDependencySearchOpenChange}
        filteredDependencyCandidates={filteredDependencyCandidates}
        selectedDependencies={selectedDependencies}
        onAddDependency={onAddDependency}
        onRemoveDependency={onRemoveDependency}
        incompleteDependencies={incompleteDependencies}
        circularDependencyDetected={circularDependencyDetected}
      />

      <WorklogSettingsModal
        open={activeModal === "settings"}
        onOpenChange={(open) => onActiveModalChange(open ? "settings" : null)}
        values={values}
        onValuesChange={onValuesChange}
        controlClassName={controlClassName}
        teamOptions={teamOptions}
        authorOptions={authorOptions}
        statusOptions={statusOptions}
      />

      <WorklogTagModal
        open={activeModal === "tags"}
        onOpenChange={(open) => onActiveModalChange(open ? "tags" : null)}
        searchControlClassName={searchControlClassName}
        tagKeywordInput={tagKeywordInput}
        onTagKeywordInputChange={onTagKeywordInputChange}
        tagSearchOpen={tagSearchOpen}
        onTagSearchOpenChange={onTagSearchOpenChange}
        filteredTagCandidates={filteredTagCandidates}
        selectedTags={selectedTags}
        onAddTag={onAddTag}
        onRemoveTag={onRemoveTag}
      />
    </>
  )
}
