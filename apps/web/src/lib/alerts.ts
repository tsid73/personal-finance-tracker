import Swal from "sweetalert2";

function isDarkMode() {
  return document.documentElement.classList.contains("dark");
}

function themedOptions() {
  return isDarkMode()
    ? {
        background: "#0f172a",
        color: "#e2e8f0"
      }
    : {
        background: "#ffffff",
        color: "#102a43"
      };
}

export async function confirmDestructiveAction(title: string, text: string, confirmButtonText: string) {
  const result = await Swal.fire({
    title,
    text,
    icon: "warning",
    showCancelButton: true,
    focusCancel: true,
    confirmButtonText,
    cancelButtonText: "Cancel",
    confirmButtonColor: "#e76f51",
    ...themedOptions()
  });

  return result.isConfirmed;
}

export async function showSuccessToast(title: string) {
  await Swal.fire({
    title,
    icon: "success",
    toast: true,
    position: "top-end",
    timer: 2200,
    timerProgressBar: true,
    showConfirmButton: false,
    ...themedOptions()
  });
}

export async function showErrorToast(title: string) {
  await Swal.fire({
    title,
    icon: "error",
    toast: true,
    position: "top-end",
    timer: 3200,
    timerProgressBar: true,
    showConfirmButton: false,
    ...themedOptions()
  });
}
