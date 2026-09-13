#include <android/log.h>
#include <jni.h>
#include <cstdint>
#include <memory>
#include <string>

#define LOG_TAG "RedRockMc"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace Microsoft {
namespace Applications {
namespace Events {

enum PiiKind : int {};
enum DataCategory : int {};
enum EventLatency : int {};
enum EventPersistence : int {};
enum status_t : int {};

struct IModule;

struct EventProperty {
  EventProperty(std::string const &, PiiKind, DataCategory);
  ~EventProperty();
};

EventProperty::EventProperty(std::string const &, PiiKind, DataCategory) {}
EventProperty::~EventProperty() = default;

struct EventProperties {
  EventProperties(std::string const &);
  ~EventProperties();
  void SetLatency(EventLatency);
  void SetProperty(std::string const &, char const *, PiiKind, DataCategory);
  void SetProperty(std::string const &, long, PiiKind, DataCategory);
  void SetPopsample(double);
  void SetPersistence(EventPersistence);
  void SetPolicyBitFlags(unsigned long);
};

EventProperties::EventProperties(std::string const &) {}
EventProperties::~EventProperties() = default;
void EventProperties::SetLatency(EventLatency) {}
void EventProperties::SetProperty(std::string const &, char const *, PiiKind, DataCategory) {}
void EventProperties::SetProperty(std::string const &, long, PiiKind, DataCategory) {}
void EventProperties::SetPopsample(double) {}
void EventProperties::SetPersistence(EventPersistence) {}
void EventProperties::SetPolicyBitFlags(unsigned long) {}

struct CorrelationVector {
  CorrelationVector();
  void Initialize(int);
  std::string GetNextValue();
  void Uninitialize();
  bool IsInitialized();
  std::string Extend();
  std::string GetValue();
  void SetValue(std::string const &);
};

CorrelationVector::CorrelationVector() = default;
void CorrelationVector::Initialize(int) {}
std::string CorrelationVector::GetNextValue() { return std::string(); }
void CorrelationVector::Uninitialize() {}
bool CorrelationVector::IsInitialized() { return false; }
std::string CorrelationVector::Extend() { return std::string(); }
std::string CorrelationVector::GetValue() { return std::string(); }
void CorrelationVector::SetValue(std::string const &) {}

struct ILogConfiguration {
  void *GetModules();
  void AddModule(char const *, std::shared_ptr<IModule> const &);
  void *operator*();
  void *operator[](char const *);
};

void *ILogConfiguration::GetModules() { return nullptr; }
void ILogConfiguration::AddModule(char const *, std::shared_ptr<IModule> const &) {}
void *ILogConfiguration::operator*() { return this; }
void *ILogConfiguration::operator[](char const *) { return this; }

struct LogManagerProvider {
  static void *Get(ILogConfiguration &, status_t &);
  static void Release(ILogConfiguration &);
};

void *LogManagerProvider::Get(ILogConfiguration &, status_t &status) {
  status = static_cast<status_t>(0);
  return nullptr;
}

void LogManagerProvider::Release(ILogConfiguration &) {}

struct DebugEventSource {
  virtual ~DebugEventSource() = default;
};

DebugEventSource keep_vtable;

}  // namespace Events
}  // namespace Applications
}  // namespace Microsoft

extern "C" int evt_api_call_default(void) { return 0; }

extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
  (void) vm;
  (void) reserved;
  LOGI("stub libmaesdk JNI_OnLoad");
  return JNI_VERSION_1_6;
}
