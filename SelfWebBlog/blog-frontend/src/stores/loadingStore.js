import { reactive } from 'vue'

let _timer = null

const state = reactive({
  isRouterLoading: false,
  isDataLoading: false,

  setRouterLoading(status) {
    this.isRouterLoading = status
    if (status) {
      if (_timer) clearTimeout(_timer)
      _timer = setTimeout(() => {
        this.isRouterLoading = false
      }, 10000)
      this.isDataLoading = false
    } else {
      if (_timer) clearTimeout(_timer)
    }
  },

  setDataLoading(status) {
    if (!this.isRouterLoading) {
      this.isDataLoading = status
    }
  }
})

export default state
