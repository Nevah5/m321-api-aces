<template>
  <main>
    <LoadingOverlay :enabled="isLoading" />
    <h1>Welcome, {{ username }}!</h1>
    <RoomManageComponent />
  </main>
</template>

<script lang="ts" setup>
import { onMounted, ref } from "vue";
import LoadingOverlay from "@/components/page/LoadingOverlay.vue";
import { playerService } from "@/api";
import type { ApiError, Player } from "@/api/types";
import { useRouter } from "vue-router";
import RoomManageComponent from "@/components/game/RoomManageComponent.vue";
import toastApi from "@/api/toastApi";

const isLoading = ref(false);
const username = ref("");
const router = useRouter();

onMounted(async () => {
  isLoading.value = true;
  const token = localStorage.getItem("token");
  if (token == null || token == "") {
    setTimeout(() => {
      isLoading.value = false;
      router.push("/login");
    }, 1000);
    return;
  }

  try {
    const player: Player = await playerService.getSelf(token);
    isLoading.value = false;
    localStorage.setItem("playerId", player.id);
    localStorage.setItem("username", player.username);
    username.value = player.username;
  } catch (e: ApiError | any) {
    if (
      e &&
      typeof e === "object" &&
      "code" in e &&
      typeof e.code === "number" &&
      e.code >= 500
    ) {
      toastApi.emit({
        title: "Server Error",
        message: "Something went wrong on our end.\n" + e.errorCode,
      });
    } else {
      toastApi.emit({
        title: "Retrieving user information failed",
        message: "Perhaps your session expired? Please try again.",
      });
    }
    isLoading.value = true;
    localStorage.clear();
    router.push("/login");
  }
});
</script>
