<?php
declare(strict_types=1);

require_once __DIR__ . '/config.php';
mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

class cls_conexion
{
    public ?mysqli $conexion = null;

    public function conectar(): mysqli
    {
        $this->conexion = mysqli_connect(host, usuario, clave, db);
        mysqli_set_charset($this->conexion, 'utf8mb4');
        return $this->conexion;
    }
}
