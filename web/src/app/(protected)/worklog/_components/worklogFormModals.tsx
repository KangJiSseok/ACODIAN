"use client"

import type {
  WorklogFormDependencyOption,
  WorklogFormTagOption,
  WorklogFormValues,
} from "../_types/worklog.types"
import { WorklogSettingsModal } from "./worklogSettingsModal"
import { WorklogTagModal } from "./worklogTagModal"

export type WorklogFormModalKey = "settings" | "tags"

type SelectOption = {
  label: string
  value: string
}

type SettingsValidationErrors = {
  actualHours?: string
}

interface WorklogFormModalsProps {
  activeModal: WorklogFormModalKey | null
  onActiveModalChange: (modal: WorklogFormModalKey | null) => void
  values: WorklogFormValues
  onValuesChange: (values: WorklogFormValues) => void
  controlClassName: string
  searchControlClassName: string
  disableTeamChange?: boolean
  teamOptions: SelectOption[]
  statusOptions: SelectOption[]
  settingsValidationErrors: SettingsValidationErrors
  actualHoursInput: string
  onActualHoursInputChange: (value: string) => void
  dependencyKeywordInput: string
  onDependencyKeywordInputChange: (value: string) => void
  dependencySearchOpen: boolean
  onDependencySearchOpenChange: (open: boolean) => void
  filteredDependencyCandidates: WorklogFormDependencyOption[]
  selectedDependencies: WorklogFormDependencyOption[]
  onAddDependency: (dependencyId: number) => void
  onRemoveDependency: (dependencyId: number) => void
  incompleteDependencies: WorklogFormDependencyOption[]
  circularDependencyDetected: boolean
  tagKeywordInput: string
  onTagKeywordInputChange: (value: string) => void
  tagSearchOpen: boolean
  onTagSearchOpenChange: (open: boolean) => void
  filteredTagCandidates: WorklogFormTagOption[]
  selectedTags: WorklogFormTagOption[]
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
  disableTeamChange,
  teamOptions,
  statusOptions,
  settingsValidationErrors,
  actualHoursInput,
  onActualHoursInputChange,
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
      <WorklogSettingsModal
        open={activeModal === "settings"}
        onOpenChange={(open) => onActiveModalChange(open ? "settings" : null)}
        values={values}
        onValuesChange={onValuesChange}
        controlClassName={controlClassName}
        disableTeamChange={disableTeamChange}
        teamOptions={teamOptions}
        statusOptions={statusOptions}
        validationErrors={settingsValidationErrors}
        actualHoursInput={actualHoursInput}
        onActualHoursInputChange={onActualHoursInputChange}
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
