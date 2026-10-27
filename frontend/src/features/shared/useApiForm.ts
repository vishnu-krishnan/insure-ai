import { zodResolver } from '@hookform/resolvers/zod'
import { useRef, useState } from 'react'
import { useForm, type DefaultValues, type FieldValues, type Path } from 'react-hook-form'
import type { z } from 'zod'

import { ApiError } from '../../api/problem'
import { describeError, type ErrorSummary } from './errors'

function hasPath(values: unknown, path: string): boolean {
  let current: unknown = values
  for (const key of path.split('.')) {
    if (typeof current !== 'object' || current === null || !(key in current)) {
      return false
    }
    current = (current as Record<string, unknown>)[key]
  }
  return true
}

export function useApiForm<T extends FieldValues>(
  schema: z.ZodType<T, T>,
  defaultValues: DefaultValues<T>,
  submit: (values: T) => Promise<void>,
  known: Record<string, string> = {},
) {
  const form = useForm<T>({ resolver: zodResolver(schema), defaultValues })
  const [formError, setFormError] = useState<ErrorSummary | null>(null)
  const inFlight = useRef(false)

  const onSubmit = form.handleSubmit(async (values) => {
    if (inFlight.current) {
      return
    }
    inFlight.current = true
    setFormError(null)
    try {
      await submit(values)
    } catch (error) {
      const fields = error instanceof ApiError ? error.errors : []
      const unmatched = fields.filter((item) => !hasPath(form.getValues(), item.field))
      fields
        .filter((item) => hasPath(form.getValues(), item.field))
        .forEach((item) => form.setError(item.field as Path<T>, { message: item.message }))
      if (fields.length === 0 || unmatched.length > 0) {
        setFormError(describeError(error, known))
      }
    } finally {
      inFlight.current = false
    }
  })

  return { form, onSubmit, formError, isSubmitting: form.formState.isSubmitting }
}
