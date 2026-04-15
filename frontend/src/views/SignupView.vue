<template>
  <main class="page-shell auth-shell">
    <div class="page-container page-container--tiny">
      <section class="auth-card">
        <div class="auth-card__hero">
          <p class="auth-card__eyebrow">Create Account</p>
          <h1 class="auth-card__title">Create your MES workspace access</h1>
          <p class="auth-card__text">
            Sign up once, then move straight into the live dashboard.
          </p>
        </div>

        <form class="auth-form" @submit.prevent="handleSubmit">
          <label class="auth-form__field">
            <span>Username</span>
            <input v-model.trim="form.username" type="text" placeholder="mes.operator" autocomplete="username" required>
          </label>

          <label class="auth-form__field">
            <span>Display name</span>
            <input v-model.trim="form.displayName" type="text" placeholder="Factory Operator" autocomplete="name" required>
          </label>

          <label class="auth-form__field">
            <span>Email</span>
            <input v-model.trim="form.email" type="email" placeholder="you@company.com" autocomplete="email" required>
          </label>

          <label class="auth-form__field">
            <span>Password</span>
            <input v-model="form.password" type="password" placeholder="At least 8 characters" autocomplete="new-password" required>
          </label>

          <label class="auth-form__field">
            <span>Confirm password</span>
            <input v-model="form.confirmPassword" type="password" placeholder="Enter password again" autocomplete="new-password" required>
          </label>

          <p v-if="errorMessage" class="auth-form__error">{{ errorMessage }}</p>

          <button type="submit" class="auth-form__submit" :disabled="submitting">
            {{ submitting ? 'Creating account...' : 'Create account' }}
          </button>
        </form>

        <p class="auth-card__footer">
          Already have an account?
          <RouterLink to="/login" class="text-link">Sign in</RouterLink>
        </p>
      </section>
    </div>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { signUp } from '@/lib/auth'

const router = useRouter()
const submitting = ref(false)
const errorMessage = ref('')
const form = reactive({
  username: '',
  displayName: '',
  email: '',
  password: '',
  confirmPassword: '',
})

async function handleSubmit() {
  submitting.value = true
  errorMessage.value = ''

  try {
    await signUp({ ...form })
    router.push('/')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Sign up failed.'
  } finally {
    submitting.value = false
  }
}
</script>
