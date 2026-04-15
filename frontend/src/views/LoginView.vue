<template>
  <main class="page-shell auth-shell">
    <div class="page-container page-container--tiny">
      <section class="auth-card">
        <div class="auth-card__hero">
          <p class="auth-card__eyebrow">MES Access</p>
          <h1 class="auth-card__title">Smart Factory Control Center</h1>
          <p class="auth-card__text">
            Sign in before entering the live factory dashboard.
          </p>
        </div>

        <form class="auth-form" @submit.prevent="handleSubmit">
          <label class="auth-form__field">
            <span>Email</span>
            <input v-model.trim="form.email" type="email" placeholder="you@company.com" autocomplete="email" required>
          </label>

          <label class="auth-form__field">
            <span>Password</span>
            <input v-model="form.password" type="password" placeholder="Enter your password" autocomplete="current-password" required>
          </label>

          <p v-if="errorMessage" class="auth-form__error">{{ errorMessage }}</p>

          <button type="submit" class="auth-form__submit" :disabled="submitting">
            {{ submitting ? 'Signing in...' : 'Sign in' }}
          </button>
        </form>

        <p class="auth-card__footer">
          Need an account?
          <RouterLink to="/signup" class="text-link">Sign up</RouterLink>
        </p>
      </section>
    </div>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { login } from '@/lib/auth'

const router = useRouter()
const submitting = ref(false)
const errorMessage = ref('')
const form = reactive({
  email: '',
  password: '',
})

async function handleSubmit() {
  submitting.value = true
  errorMessage.value = ''

  try {
    await login({
      email: form.email,
      password: form.password,
    })
    router.push('/')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Login failed.'
  } finally {
    submitting.value = false
  }
}
</script>
