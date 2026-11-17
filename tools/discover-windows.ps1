# Read-only discovery comparison. No camera is opened or reconfigured.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$wifiDll = Join-Path $projectRoot 'vendor/toupnamsdk/win/x64/toupnam.dll'
$gigeDll = [IO.Path]::GetFullPath((Join-Path $projectRoot '../../win/x64/maguscam.dll'))
$source = @"
using System;
using System.Runtime.InteropServices;
public static class DiscoveryProbe {
    [DllImport(@"$wifiDll", CallingConvention=CallingConvention.StdCall)]
    public static extern void Toupnam_Init(IntPtr callback, IntPtr context);
    [DllImport(@"$wifiDll", CallingConvention=CallingConvention.StdCall)]
    public static extern void Toupnam_PriFlag(uint flags, uint mask);
    [DllImport(@"$wifiDll", CallingConvention=CallingConvention.StdCall)]
    public static extern uint Toupnam_Enum(IntPtr array, uint capacity);
    [DllImport(@"$wifiDll", CallingConvention=CallingConvention.StdCall)]
    public static extern void Toupnam_Fini();
    [DllImport(@"$gigeDll", CallingConvention=CallingConvention.StdCall)]
    public static extern int Maguscam_GigeEnableV2(IntPtr callback, IntPtr context, [MarshalAs(UnmanagedType.LPStr)] string options);
    [DllImport(@"$gigeDll", CallingConvention=CallingConvention.StdCall)]
    public static extern uint Maguscam_EnumV2(IntPtr array);
}
"@
Add-Type -TypeDefinition $source
[DiscoveryProbe]::Toupnam_PriFlag(2, 2)
[DiscoveryProbe]::Toupnam_Init([IntPtr]::Zero, [IntPtr]::Zero)
$gigeResult = [DiscoveryProbe]::Maguscam_GigeEnableV2([IntPtr]::Zero, [IntPtr]::Zero, 'wifi')
Write-Output ('GigE initialization: 0x{0:X8}' -f $gigeResult)
try {
    for ($attempt = 0; $attempt -lt 10; ++$attempt) {
        Start-Sleep -Seconds 1
        $wifiCount = [DiscoveryProbe]::Toupnam_Enum([IntPtr]::Zero, 0)
        $gigeCount = [DiscoveryProbe]::Maguscam_EnumV2([IntPtr]::Zero)
        Write-Output "Wi-Fi cameras: $wifiCount; GigE cameras: $gigeCount"
        if ($wifiCount -gt 0) {
            # ToupnamDevice from the bundled SDK header: six char[64], char[256],
            # two uint32, then 64 ranges containing four int32 each = 1672 bytes.
            $buffer = [Runtime.InteropServices.Marshal]::AllocHGlobal(1672 * $wifiCount)
            try {
                $actualCount = [DiscoveryProbe]::Toupnam_Enum($buffer, $wifiCount)
                for ($i = 0; $i -lt [Math]::Min($actualCount, $wifiCount); ++$i) {
                    $model = [Runtime.InteropServices.Marshal]::PtrToStringAnsi([IntPtr]::Add($buffer, $i * 1672 + 192))
                    $address = [Runtime.InteropServices.Marshal]::PtrToStringAnsi([IntPtr]::Add($buffer, $i * 1672 + 320))
                    Write-Output "Discovered: $model at $address"
                }
            } finally { [Runtime.InteropServices.Marshal]::FreeHGlobal($buffer) }
            break
        }
    }
} finally { [DiscoveryProbe]::Toupnam_Fini() }
