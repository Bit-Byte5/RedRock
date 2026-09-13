#include <android/log.h>
#include <jni.h>
#include <stdint.h>

#define LOG_TAG "RedRockMc"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

#define FAIL() (-1)

int32_t PFLobbyAddMember(void) { return FAIL(); }
int32_t PFLobbyForceRemoveMember(void) { return FAIL(); }
int32_t PFLobbyGetAccessPolicy(void) { return FAIL(); }
int32_t PFLobbyGetLobbyId(void) { return FAIL(); }
int32_t PFLobbyGetLobbyProperty(void) { return FAIL(); }
int32_t PFLobbyGetMaxMemberCount(void) { return FAIL(); }
int32_t PFLobbyGetMemberConnectionStatus(void) { return FAIL(); }
int32_t PFLobbyGetMemberProperty(void) { return FAIL(); }
int32_t PFLobbyGetMembers(void) { return FAIL(); }
int32_t PFLobbyGetOwner(void) { return FAIL(); }
int32_t PFLobbyGetServerProperty(void) { return FAIL(); }
int32_t PFLobbyLeave(void) { return FAIL(); }
int32_t PFLobbySendInvite(void) { return FAIL(); }
int32_t PFMultiplayerConnectToLobby(void) { return FAIL(); }
int32_t PFMultiplayerCreateAndJoinLobby(void) { return FAIL(); }
int32_t PFMultiplayerFindLobbies(void) { return FAIL(); }
int32_t PFMultiplayerFinishProcessingLobbyStateChanges(void) { return FAIL(); }
int32_t PFMultiplayerInitialize(void) { return FAIL(); }
int32_t PFMultiplayerJoinArrangedLobby(void) { return FAIL(); }
int32_t PFMultiplayerJoinLobby(void) { return FAIL(); }
int32_t PFMultiplayerSetEntityToken(void) { return FAIL(); }
int32_t PFMultiplayerStartListeningForLobbyInvites(void) { return FAIL(); }
int32_t PFMultiplayerStartProcessingLobbyStateChanges(void) { return FAIL(); }
int32_t PFMultiplayerStopListeningForLobbyInvites(void) { return FAIL(); }
int32_t PFMultiplayerUninitialize(void) { return 0; }

const char *PFMultiplayerGetErrorMessage(int32_t error) {
  (void) error;
  return "redrock playfab stub";
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
  (void) vm;
  (void) reserved;
  LOGI("stub libPlayFabMultiplayer JNI_OnLoad");
  return JNI_VERSION_1_6;
}
