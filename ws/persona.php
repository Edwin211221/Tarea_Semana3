<?php
declare(strict_types=1);

require_once __DIR__ . '/conex.php';
header('Content-Type: application/json; charset=utf-8');

function responder(int $codigoHttp, array $respuesta): never
{
    http_response_code($codigoHttp);
    echo json_encode($respuesta, JSON_UNESCAPED_UNICODE);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    responder(405, ['estado' => false, 'mensaje' => 'Método no permitido.']);
}

$datos = json_decode(file_get_contents('php://input'), true);
if (!is_array($datos)) {
    responder(400, ['estado' => false, 'mensaje' => 'La solicitud no contiene JSON válido.']);
}

$accion = $datos['accion'] ?? '';
if (!in_array($accion, ['loggin', 'verificarCedula'], true)) {
    responder(400, ['estado' => false, 'mensaje' => 'Acción no válida.']);
}

if ($accion === 'loggin') {
    $usuario = $datos['usuario'] ?? null;
    $clave = $datos['clave'] ?? null;
    if (!is_string($usuario) || $usuario === '' || !is_string($clave) || $clave === '') {
        responder(400, ['estado' => false, 'mensaje' => 'Ingresa usuario y contraseña.']);
    }
} else {
    $cedula = $datos['cedula'] ?? null;
    if (!is_string($cedula) || preg_match('/^[0-9]{10}$/', $cedula) !== 1) {
        responder(400, ['estado' => false, 'mensaje' => 'La cédula debe tener exactamente 10 dígitos.']);
    }
}

try {
    $conexion = (new cls_conexion())->conectar();

    if ($accion === 'loggin') {
        $sentencia = mysqli_prepare(
            $conexion,
            'SELECT codigo FROM persona WHERE usuario = ? AND clave = ? LIMIT 1'
        );
        mysqli_stmt_bind_param($sentencia, 'ss', $usuario, $clave);
        mysqli_stmt_execute($sentencia);
        mysqli_stmt_bind_result($sentencia, $codigo);
        $encontrado = mysqli_stmt_fetch($sentencia);
        mysqli_stmt_close($sentencia);
        mysqli_close($conexion);

        if ($encontrado) {
            responder(200, ['estado' => true, 'codigo' => $codigo]);
        }

        responder(200, ['estado' => false, 'mensaje' => 'Usuario o clave incorrecto.']);
    }

    $sentencia = mysqli_prepare(
        $conexion,
        'SELECT codigo FROM persona WHERE cedula = ? LIMIT 1'
    );
    mysqli_stmt_bind_param($sentencia, 's', $cedula);
    mysqli_stmt_execute($sentencia);
    mysqli_stmt_store_result($sentencia);
    $existe = mysqli_stmt_num_rows($sentencia) > 0;
    mysqli_stmt_close($sentencia);
    mysqli_close($conexion);

    responder(200, ['estado' => $existe]);
} catch (Throwable $error) {
    error_log('persona.php: ' . $error->getMessage());
    responder(500, ['estado' => false, 'mensaje' => 'No se pudo consultar la base de datos.']);
}
