Imports System.IO
Imports System.Drawing.Imaging
Imports System.Runtime.InteropServices

Public Class Form1
    Private Delegate Sub DelegateOnEvent(nEvent As UInteger, nPara As UInteger)
    Private dev_ As Toupnam.device
    Private cam_ As Toupnam = Nothing
    Private bmp_ As Bitmap = Nothing
    Private onevent_ As DelegateOnEvent = Nothing
    Private count_ As UInteger = 0

    Public Sub New()
        InitializeComponent()
        pictureBox1.Width = ClientRectangle.Right - pictureBox1.Left - button1.Top
        pictureBox1.Height = ClientRectangle.Height - 2 * button1.Top
        Toupnam.Init(Nothing)
    End Sub

    Private Sub Form_SizeChanged(sender As Object, e As EventArgs) Handles MyBase.SizeChanged
        pictureBox1.Width = ClientRectangle.Right - pictureBox1.Left - button1.Top
        pictureBox1.Height = ClientRectangle.Height - 2 * button1.Top
    End Sub

    ' this is called by internal thread of toupnam.dll which Is Not the same of UI thread.
    ' Why we use BeginInvoke, Please see:
    ' http//msdn.microsoft.com/en-us/magazine/cc300429.aspx
    ' http://msdn.microsoft.com/en-us/magazine/cc188732.aspx
    ' http://stackoverflow.com/questions/1364116/avoiding-the-woes-of-invoke-begininvoke-in-cross-thread-winform-event-handling
    '
    Private Sub OnEventCallback(nEvent As UInteger, nPara As UInteger, ByRef pExtra As Toupnam.eventextra)
        BeginInvoke(onevent_, New Object() {nEvent, nPara})
    End Sub

    ' this run in the UI thread
    Private Sub OnEvent(nEvent As UInteger, nPara As UInteger)
        If cam_ IsNot Nothing Then
            Select Case nEvent
                Case Toupnam.EVENT_IMAGE
                    If bmp_ IsNot Nothing Then
                        Dim w As UInteger = 0, h As UInteger = 0
                        Dim bOK As Boolean = False
                        Try
                            Dim bmpdata As BitmapData = bmp_.LockBits(New Rectangle(0, 0, bmp_.Width, bmp_.Height), ImageLockMode.WriteOnly, bmp_.PixelFormat)
                            Try
                                bOK = cam_.PullImage(bmpdata.Scan0, 24, w, h)
                            Finally
                                bmp_.UnlockBits(bmpdata)
                            End Try
                        Catch ex As Exception
                            MessageBox.Show(ex.ToString())
                        End Try
                        If bOK Then
                            pictureBox1.Invalidate()
                        End If
                    End If
                Case Toupnam.EVENT_PARA
                    Select Case nPara
                        Case Toupnam.PARA_EXPOTIME
                            Dim val As Integer = 0
                            If cam_.get_Para(Toupnam.PARA_EXPOTIME, val) >= 0 Then
                                trackBar1.Value = val
                                label1.Text = (val / 1000).ToString()
                            End If
                        Case Toupnam.PARA_WBREDGAIN
                            Dim r As Integer = 0
                            If cam_.get_Para(Toupnam.PARA_WBREDGAIN, r) >= 0 Then
                                TrackBar2.Value = r
                                Label2.Text = r.ToString()
                            End If
                        Case Toupnam.PARA_WBGREENGAIN
                            Dim g As Integer = 0
                            If cam_.get_Para(Toupnam.PARA_WBGREENGAIN, g) >= 0 Then
                                TrackBar3.Value = g
                                Label3.Text = g.ToString()
                            End If
                        Case Toupnam.PARA_WBBLUEGAIN
                            Dim b As Integer = 0
                            If cam_.get_Para(Toupnam.PARA_WBBLUEGAIN, b) >= 0 Then
                                TrackBar4.Value = b
                                Label4.Text = b.ToString()
                            End If
                        Case Toupnam.PARA_AWB
                            Dim awb As Integer = 0
                            If cam_.get_Para(Toupnam.PARA_AWB, awb) >= 0 Then
                                CheckBox2.Checked = (awb = 1)
                                TrackBar2.Enabled = (awb = 0)
                                TrackBar3.Enabled = (awb = 0)
                                TrackBar4.Enabled = (awb = 0)
                            End If
                        Case Toupnam.PARA_AEXPO
                            Dim aexpo As Integer = 0
                            If cam_.get_Para(Toupnam.PARA_AEXPO, aexpo) >= 0 Then
                                checkBox1.Checked = (aexpo = 1)
                                trackBar1.Enabled = (aexpo = 0)
                            End If
                    End Select
                Case Toupnam.EVENT_ERROR
                    cam_.Close()
                    cam_ = Nothing
                    MessageBox.Show("Generic error.")
            End Select
        End If
    End Sub

    Private Overloads Sub OnClosing(sender As Object, e As FormClosingEventArgs) Handles MyBase.FormClosing
        If cam_ IsNot Nothing Then
            cam_.Close()
            cam_ = Nothing
        End If
        Toupnam.Fini()
    End Sub

    Private Overloads Sub OnClosed(sender As Object, e As FormClosedEventArgs) Handles MyBase.FormClosed
        Toupnam.Fini()
    End Sub

    Private Sub Form1_Load(sender As Object, e As EventArgs) Handles MyBase.Load
        Me.button2.Enabled = False
        Me.trackBar1.Enabled = False
        Me.checkBox1.Enabled = False
        Me.CheckBox2.Enabled = False
        Me.TrackBar2.Enabled = False
        Me.TrackBar3.Enabled = False
        Me.TrackBar4.Enabled = False
    End Sub

    Private Sub OnStart(sender As Object, e As EventArgs) Handles button1.Click
        If cam_ IsNot Nothing Then
            Return
        End If

        Dim arr As Toupnam.device() = Toupnam.[Enum]()
        If arr.Length <= 0 Then
            MessageBox.Show("No camera found.")
        Else
            dev_ = arr(0)
            cam_ = Toupnam.Open(dev_.id)
            If cam_ IsNot Nothing Then
                Me.checkBox1.Enabled = True
                Me.button2.Enabled = True
                Me.CheckBox2.Enabled = True

                trackBar1.SetRange(arr(0).r(Toupnam.PARA_EXPOTIME).imin, arr(0).r(Toupnam.PARA_EXPOTIME).imax)
                TrackBar2.SetRange(arr(0).r(Toupnam.PARA_WBREDGAIN).imin, arr(0).r(Toupnam.PARA_WBREDGAIN).imax)
                TrackBar3.SetRange(arr(0).r(Toupnam.PARA_WBGREENGAIN).imin, arr(0).r(Toupnam.PARA_WBGREENGAIN).imax)
                TrackBar4.SetRange(arr(0).r(Toupnam.PARA_WBBLUEGAIN).imin, arr(0).r(Toupnam.PARA_WBBLUEGAIN).imax)

                Dim w As Integer = 0, h As Integer = 0
                If cam_.get_Size(w, h) >= 0 Then
                    onevent_ = New DelegateOnEvent(AddressOf OnEvent)
                    bmp_ = New Bitmap(w, h, PixelFormat.Format24bppRgb)
                    If cam_.StartPullModeWithCallback(New Toupnam.DelegateEventCallback(AddressOf OnEventCallback)) < 0 Then
                        MessageBox.Show("Failed to start camera.")
                    Else
                        OnEvent(Toupnam.EVENT_PARA, Toupnam.PARA_EXPOTIME)
                        OnEvent(Toupnam.EVENT_PARA, Toupnam.PARA_WBREDGAIN)
                        OnEvent(Toupnam.EVENT_PARA, Toupnam.PARA_WBGREENGAIN)
                        OnEvent(Toupnam.EVENT_PARA, Toupnam.PARA_WBBLUEGAIN)
                        OnEvent(Toupnam.EVENT_PARA, Toupnam.PARA_AWB)
                        OnEvent(Toupnam.EVENT_PARA, Toupnam.PARA_AEXPO)
                    End If
                End If
            End If
        End If
    End Sub

    Private Sub checkBox1_CheckedChanged(sender As Object, e As EventArgs) Handles checkBox1.CheckedChanged
        If cam_ IsNot Nothing Then
            cam_.put_Para(Toupnam.PARA_AEXPO, If(checkBox1.Checked, 1, 0))
        End If
    End Sub

    Private Sub checkBox2_CheckedChanged(sender As Object, e As EventArgs) Handles CheckBox2.CheckedChanged
        If cam_ IsNot Nothing Then
            cam_.put_Para(Toupnam.PARA_AWB, If(CheckBox2.Checked, 1, 0))
        End If
    End Sub

    Private Sub OnExpoValueChange(sender As Object, e As EventArgs) Handles trackBar1.ValueChanged
        If Not checkBox1.Checked AndAlso cam_ IsNot Nothing Then
            cam_.put_Para(Toupnam.PARA_EXPOTIME, trackBar1.Value)
        End If
    End Sub

    Private Sub OnWBRedChange(sender As Object, e As EventArgs) Handles TrackBar2.ValueChanged
        If Not CheckBox2.Checked AndAlso cam_ IsNot Nothing Then
            cam_.put_Para(Toupnam.PARA_WBREDGAIN, TrackBar2.Value)
        End If
    End Sub

    Private Sub OnWBGreenChange(sender As Object, e As EventArgs) Handles TrackBar3.ValueChanged
        If Not CheckBox2.Checked AndAlso cam_ IsNot Nothing Then
            cam_.put_Para(Toupnam.PARA_WBGREENGAIN, TrackBar3.Value)
        End If
    End Sub

    Private Sub OnWBBlueChange(sender As Object, e As EventArgs) Handles TrackBar4.ValueChanged
        If Not CheckBox2.Checked AndAlso cam_ IsNot Nothing Then
            cam_.put_Para(Toupnam.PARA_WBBLUEGAIN, TrackBar4.Value)
        End If
    End Sub

    Private Sub OnCaptureCallback(result As Integer, pData As IntPtr, nLength As Integer, ByRef info As Toupnam.bitmapinfo)
        If pData <> IntPtr.Zero AndAlso nLength > 0 Then
            Try
                count_ = count_ + 1
                Using file As FileStream = New FileStream(String.Format("demovb{0}.jpg", count_), FileMode.Create, FileAccess.Write)
                    If file IsNot Nothing Then
                        Dim bytes As Byte() = New Byte(nLength - 1) {}
                        Marshal.Copy(pData, bytes, 0, nLength)
                        file.Write(bytes, 0, nLength)
                    End If
                End Using
            Catch ex As Exception
                MessageBox.Show(ex.ToString())
            End Try
        End If
    End Sub

    Private Sub pictureBox1_Paint(sender As Object, e As PaintEventArgs) Handles pictureBox1.Paint
        Dim g As Graphics = e.Graphics
        If (bmp_ IsNot Nothing) Then
            g.DrawImage(bmp_, pictureBox1.ClientRectangle)
            g.DrawString(label4.Text, Me.Font, SystemBrushes.ControlText, 0, 0)
        End If
    End Sub

    Private Sub OnCapture(sender As Object, e As EventArgs) Handles button2.Click
        If cam_ IsNot Nothing Then
            If dev_.flag And Toupnam.FLAG_CAPTURE <> 0 Then
                cam_.Capture(Nothing, New Toupnam.DelegateCaptureCallback(AddressOf OnCaptureCallback))
            ElseIf bmp_ IsNot Nothing Then
                count_ = count_ + 1
                bmp_.Save(String.Format("demovb{0}.bmp", count_), ImageFormat.Bmp)
            End If
        End If
    End Sub
End Class
