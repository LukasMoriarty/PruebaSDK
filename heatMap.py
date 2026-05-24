import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
from scipy.stats import gaussian_kde
import os


def generar_mapa_calor(ruta_csv):
    """
    Genera un mapa de calor suave (KDE) controlando el radio del halo (bw_method)
    """
    if not os.path.exists(ruta_csv):
        print(f"Error: El archivo {ruta_csv} no existe.")
        return

    # Cargar el archivo CSV
    df = pd.read_csv(ruta_csv)
    if "RawX" not in df.columns or "RawY" not in df.columns:
        print("Error: El archivo CSV no contiene la columna 'RawX' o 'RawY'")
        return

    df = df.dropna(subset=["RawX", "RawY"])
    x = df["RawX"].tolist()
    y = df["RawY"].tolist()

    # Dimensiones fijas de la pantalla
    ancho_pantalla = 1080
    alto_pantalla = 2400

    # Fondo en negro
    plt.style.use("dark_background")
    fig, ax = plt.subplots(figsize=(6, 12), facecolor="black")
    ax.set_facecolor("black")

    # Calcular la densidad (KDE)
    # bw_method=0.15 obliga a que el calor se concentre en el punto
    # y no se expanda
    k = gaussian_kde(np.vstack([x, y]), bw_method=0.15)

    # Creacion de gridilla
    xi, yi = np.mgrid[0:ancho_pantalla:250j, 0:alto_pantalla:555j]
    zi = k(np.vstack([xi.flatten(), yi.flatten()]))
    zi_shaped = zi.reshape(xi.shape)

    plt.imshow(
        zi_shaped,
        origin="lower",
        extent=[0, ancho_pantalla, 0, alto_pantalla],
        aspect="auto",
        cmap="inferno",
    )

    # Invertir el eje Y
    plt.gca().invert_yaxis()

    plt.title(
        "Mapa de Calor de Densidad Suave\nResolución de Pantalla: 1080 x 2400",
        fontsize=12,
        pad=15,
        color="white",
    )
    plt.xlabel("Eje X (Píxeles)", fontsize=10, color="gray")
    plt.ylabel("Eje Y (Píxeles)", fontsize=10, color="gray")

    plt.grid(True, linestyle=":", alpha=0.1, color="white")

    # Barra de color lateral estilizada
    barra_color = plt.colorbar(orientation="vertical", pad=0.04)
    barra_color.set_label(
        "Intensidad de Toques", fontsize=10, color="white", labelpad=10
    )
    barra_color.ax.yaxis.set_tick_params(color="white")
    plt.setp(plt.getp(barra_color.ax, "yticklabels"), color="white")

    plt.tight_layout()

    # Guardar el heap map
    nombre_salida = "heat_map.png"
    plt.savefig(nombre_salida, dpi=300, facecolor=fig.get_facecolor(), edgecolor="none")
    print(f"¡Mapa de calor generado como: {nombre_salida}!")
    plt.close()


if __name__ == "__main__":
    generar_mapa_calor("soim_events_export.csv")
